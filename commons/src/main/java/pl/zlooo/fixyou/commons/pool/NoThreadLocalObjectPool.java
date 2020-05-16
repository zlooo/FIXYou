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

package pl.zlooo.fixyou.commons.pool;

import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.commons.utils.UnsafeAccessor;

import java.util.function.Supplier;

@Slf4j
public class NoThreadLocalObjectPool<T extends AbstractPoolableObject> extends AbstractArrayBasedObjectPool<T> {

    private static final int FLOOR_LOG_2_CALCULATION_BASE = 31;
    private static final String GET_OBJECT_LOG = "Grabbing object {} from pool {}. Object resides in {}";

    private final long baseAddress;
    private final int tailAdjustment;


    @SuppressWarnings("unchecked")
    public NoThreadLocalObjectPool(int poolSize, Supplier<T> objectSupplier, Class<T> clazz) {
        super(poolSize, objectSupplier, clazz);
        baseAddress = UnsafeAccessor.UNSAFE.arrayBaseOffset(objectArray.getClass());
        final long arrayIndexScale = UnsafeAccessor.UNSAFE.arrayIndexScale(objectArray.getClass());
        tailAdjustment = FLOOR_LOG_2_CALCULATION_BASE - Integer.numberOfLeadingZeros((int) arrayIndexScale);
    }

    @Override
    public T getAndRetain() {
        int localTakePointer;
        while (objectPutPosition != (localTakePointer = objGetPosition)) {
            final int index = localTakePointer & mask;
            final T pooledObject = objectArray[index];
            if (pooledObject != null && UnsafeAccessor.UNSAFE.compareAndSwapObject(objectArray, (index << tailAdjustment) + baseAddress, pooledObject, null)) {
                objGetPosition = localTakePointer + 1;
                if (pooledObject.getState().compareAndSet(AbstractPoolableObject.AVAILABLE_STATE, AbstractPoolableObject.IN_USE_STATE)) {
                    pooledObject.retain();
                    log.debug(GET_OBJECT_LOG, pooledObject, this, "object array");
                    return pooledObject;
                }
            }
        }

        synchronized (this) { //yeah looks shitty, but it should not come to this. If we have to resize the pool is too small!!!!
            resizeObjectArray();
        }
        return getAndRetain();
    }

    @Override
    public void returnObject(T objectToBeReturned) {
        log.debug("Returning object {} to pool {}", objectToBeReturned, this);
        final int localPosition = objectPutPosition;
        final long index = ((localPosition & mask) << tailAdjustment) + baseAddress;
        if (objectToBeReturned.getState().compareAndSet(AbstractPoolableObject.IN_USE_STATE, AbstractPoolableObject.AVAILABLE_STATE)) {
            UnsafeAccessor.UNSAFE.putOrderedObject(objectArray, index, objectToBeReturned);
            objectPutPosition = localPosition + 1;
        } else {
            throw new IllegalStateException(
                    "Unexpected object state, expecting in use(" + AbstractPoolableObject.IN_USE_STATE + ") but got " + objectToBeReturned.getState().get());
        }
    }

    @Override
    public void close() {
        for (int i = 0; i < objectArray.length; i++) {
            objectArray[i].close();
            objectArray[i] = null;
        }
    }
}
