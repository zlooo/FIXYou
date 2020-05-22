/*
 * Copyright 2013 peter.lawrey Lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original file
 * https://github.com/peter-lawrey/TransFIX/blob/master/src/main/java/net/openhft/fix/include/util/FixMessagePool.java
 *
 * Modifications copyright (C) 2020 zlooo
 */

package io.github.zlooo.fixyou.commons.pool;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Totally <B>NOT THREAD SAFE</B> object pool tailored for {@link io.github.zlooo.fixyou.parser.model.FixMessage} use case. Basic idea is that we do not need any kind of synchronization/thread safety(netty events in single fix session are
 * processed sequentially) and 99% of cases will not need more than 2 objects taken out of pool
 */
@Slf4j
public class DefaultObjectPool<T extends AbstractPoolableObject> extends AbstractArrayBasedObjectPool<T> {

    private static final String GET_OBJECT_LOG = "Grabbing object {} from pool {}. Object resides in {}";
    //the idea is that 99% of cases will need at most 2 objects taken from pool
    private final T firstObject;
    private final T secondObject;

    @SuppressWarnings("unchecked")
    public DefaultObjectPool(int poolSize, Supplier<T> objectSupplier, Class<T> clazz) {
        super(poolSize, objectSupplier, clazz);
        firstObject = objectSupplier.get();
        secondObject = objectSupplier.get();
        firstObject.setPool(this);
        secondObject.setPool(this);
    }

    @Override
    public T getAndRetain() {
        if (firstObject.getState().compareAndSet(AbstractPoolableObject.AVAILABLE_STATE, AbstractPoolableObject.IN_USE_STATE)) {
            firstObject.retain();
            log.debug(GET_OBJECT_LOG, firstObject, this, "local var 1");
            return firstObject;
        }
        if (secondObject.getState().compareAndSet(AbstractPoolableObject.AVAILABLE_STATE, AbstractPoolableObject.IN_USE_STATE)) {
            secondObject.retain();
            log.debug(GET_OBJECT_LOG, firstObject, this, "local var 2");
            return secondObject;
        }

        int localTakePointer;
        while (objectPutPosition != (localTakePointer = objGetPosition)) {
            final int index = localTakePointer & mask;
            final T pooledObject = objectArray[index];
            if (pooledObject != null) {
                objGetPosition = localTakePointer + 1;
                if (pooledObject.getState().compareAndSet(AbstractPoolableObject.AVAILABLE_STATE, AbstractPoolableObject.IN_USE_STATE)) {
                    objectArray[index] = null;
                    pooledObject.retain();
                    log.debug(GET_OBJECT_LOG, pooledObject, this, "object array");
                    return pooledObject;
                } else {
                    log.warn("Item is present in array but has unexpected state! Are you sure you're using this class in single thread?");
                }
            }
        }
        resizeObjectArray();
        return getAndRetain();
    }

    @Override
    public void returnObject(T objectToBeReturned) {
        log.debug("Returning object {} to pool {}", objectToBeReturned, this);

        if (!objectToBeReturned.getState().compareAndSet(AbstractPoolableObject.IN_USE_STATE, AbstractPoolableObject.AVAILABLE_STATE)) {
            throw buildIllegalStateException(objectToBeReturned);
        } else {
            if (objectToBeReturned != firstObject && objectToBeReturned != secondObject) {
                final int index = objectPutPosition & mask;
                objectArray[index] = objectToBeReturned;
                objectPutPosition++;
            }
        }
    }

    @Override
    public void close() {
        firstObject.close();
        firstObject.getState().set(AbstractPoolableObject.IN_USE_STATE);
        secondObject.close();
        secondObject.getState().set(AbstractPoolableObject.IN_USE_STATE);
        for (int i = 0; i < objectArray.length; i++) {
            objectArray[i].close();
            objectArray[i] = null;
        }
    }

    public boolean areAllObjectsReturned() {
        boolean nonNull = true;
        boolean available = true;
        for (final T poolObject : objectArray) {
            final boolean isPoolObjectNonNull = poolObject != null;
            nonNull &= isPoolObjectNonNull;
            if (isPoolObjectNonNull) {
                available &= poolObject.getState().get() == AbstractPoolableObject.AVAILABLE_STATE;
            }
        }
        return firstObject.getState().get() == AbstractPoolableObject.AVAILABLE_STATE && secondObject.getState().get() == AbstractPoolableObject.AVAILABLE_STATE && nonNull && available;
    }

    private static <T extends AbstractPoolableObject> IllegalStateException buildIllegalStateException(T objectToBeReturned) {
        return new IllegalStateException(
                "Unexpected object state, expecting in use(" + AbstractPoolableObject.IN_USE_STATE + ") but got " + objectToBeReturned.getState().get());
    }
}
