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

import io.github.zlooo.fixyou.utils.UnsafeAccessor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@Slf4j
public class ArrayBackedObjectPool<T extends AbstractPoolableObject> extends AbstractArrayBackedObjectPool<T> {

    private static final int FLOOR_LOG_2_CALCULATION_BASE = 31;

    private final long baseAddress;
    private final int tailAdjustment;


    @SuppressWarnings("unchecked")
    public ArrayBackedObjectPool(int poolSize, Supplier<T> objectSupplier, Class<T> clazz, int maxSize) {
        super(poolSize, objectSupplier, clazz, maxSize);
        baseAddress = UnsafeAccessor.UNSAFE.arrayBaseOffset(objectArray.getClass());
        final long arrayIndexScale = UnsafeAccessor.UNSAFE.arrayIndexScale(objectArray.getClass());
        tailAdjustment = FLOOR_LOG_2_CALCULATION_BASE - Integer.numberOfLeadingZeros((int) arrayIndexScale);
    }

    @Override
    public T getAndRetain() {
        final T objectFromPool = tryGetAndRetain();
        if (objectFromPool == null) {
            synchronized (this) { //yeah looks shitty, but it should not come to this. If we have to resize the pool is too small!!!!
                resizeObjectArray();
            }
            return getAndRetain();
        } else {
            return objectFromPool;
        }
    }

    @Nullable
    @Override
    public T tryGetAndRetain() {
        int localTakePointer;
        while (objectPutPosition != (localTakePointer = objGetPosition)) {
            final int index = localTakePointer & mask;
            final T pooledObject = objectArray[index];
            if (pooledObject != null && UnsafeAccessor.UNSAFE.compareAndSwapObject(objectArray, ((long) index << tailAdjustment) + baseAddress, pooledObject, null)) {
                objGetPosition = localTakePointer + 1;
                if (pooledObject.getState().compareAndSet(AbstractPoolableObject.AVAILABLE_STATE, AbstractPoolableObject.IN_USE_STATE)) {
                    pooledObject.retain();
                    log.debug(GET_OBJECT_LOG, pooledObject, pooledObject.hashCode(), this, "object array");
                    return pooledObject;
                }
            }
        }
        return null;
    }

    @Override
    public void returnObject(T objectToBeReturned) {
        if (objectToBeReturned.getState().compareAndSet(AbstractPoolableObject.IN_USE_STATE, AbstractPoolableObject.AVAILABLE_STATE)) {
            log.debug(RETURN_OBJECT_LOG, objectToBeReturned, objectToBeReturned.hashCode(), this);
            final int localPosition = objectPutPosition;
            final long index = ((long) (localPosition & mask) << tailAdjustment) + baseAddress;
            UnsafeAccessor.UNSAFE.putOrderedObject(objectArray, index, objectToBeReturned);
            objectPutPosition = localPosition + 1;
        } else if (objectToBeReturned.getState().get() != AbstractPoolableObject.AVAILABLE_STATE) {
            throw new IllegalStateException(
                    "Unexpected object state, expecting in use(" + AbstractPoolableObject.IN_USE_STATE + ") but got " + objectToBeReturned.getState().get() + ". Object details " + objectToBeReturned + "@" + objectToBeReturned.hashCode());
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
        return nonNull && available;
    }

    @Override
    public void close() {
        for (int i = 0; i < objectArray.length; i++) {
            final T pooledObject = objectArray[i];
            if (pooledObject != null) {
                pooledObject.close();
                objectArray[i] = null;
            }
        }
    }
}
