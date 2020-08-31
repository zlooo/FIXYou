package io.github.zlooo.fixyou.commons.pool;

import io.github.zlooo.fixyou.commons.utils.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.util.function.Supplier;

@Slf4j
abstract class AbstractArrayBackedObjectPool<T extends AbstractPoolableObject> implements ObjectPool<T> {

    protected static final String GET_OBJECT_LOG = "Grabbing object {}@{} from pool {}. Object resides in {}";
    protected static final String RETURN_OBJECT_LOG = "Returning object {}@{} to pool {}";

    protected int objGetPosition;
    protected int objectPutPosition;
    protected final T[] objectArray;
    protected final Supplier<T> objectSupplier;
    protected final int mask;
    protected final Class<T> elementClass;
    protected final int maxSize;

    protected AbstractArrayBackedObjectPool(int poolSize, Supplier<T> objectSupplier, Class<T> clazz, int maxSize) {
        this.maxSize = maxSize;
        this.objectSupplier = objectSupplier;
        int currentSize = 1;
        while (currentSize < poolSize) {
            currentSize = currentSize << 1;
        }
        objectArray = (T[]) Array.newInstance(clazz, currentSize);
        for (int i = 0; i < currentSize; i++) {
            final T pooledObject = objectSupplier.get();
            pooledObject.setPool(this);
            objectArray[i] = pooledObject;
        }
        mask = currentSize - 1;
        objectPutPosition = currentSize;
        elementClass = clazz;
    }

    protected boolean resizeObjectArray() {
        final int currentSize = objectArray.length;
        final int newSize = currentSize << 1;
        if (newSize > maxSize) {
            log.warn("New size {} would exceed max pool size, {}. Not resizing", newSize, maxSize);
            return false;
        } else {
            log.warn("Nothing available in pool of {}, size {}. Consider making it bigger. Resizing it to {}", elementClass, currentSize, newSize);
            final T[] newObjectArray = (T[]) Array.newInstance(elementClass, newSize);
            System.arraycopy(objectArray, 0, newObjectArray, 0, currentSize);
            for (int i = currentSize; i < newSize; i++) {
                final T pooledObject = objectSupplier.get();
                pooledObject.setPool(this);
                newObjectArray[i] = pooledObject;
            }
            ReflectionUtils.setFinalField(this, "mask", newSize - 1);
            ReflectionUtils.setFinalField(this, "objectArray", newObjectArray);
            if (objectPutPosition == currentSize) {
                objectPutPosition = newSize;
            }
            return true;
        }
    }
}
