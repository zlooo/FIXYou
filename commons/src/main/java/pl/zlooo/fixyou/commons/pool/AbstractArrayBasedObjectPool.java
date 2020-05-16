package pl.zlooo.fixyou.commons.pool;

import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.commons.utils.ReflectionUtils;

import java.lang.reflect.Array;
import java.util.function.Supplier;

@Slf4j
abstract class AbstractArrayBasedObjectPool<T extends AbstractPoolableObject> implements ObjectPool<T> {

    protected int objGetPosition;
    protected int objectPutPosition;
    protected final T[] objectArray;
    protected final Supplier<T> objectSupplier;
    protected final int mask;
    protected final Class<T> elementClass;

    protected AbstractArrayBasedObjectPool(int poolSize, Supplier<T> objectSupplier, Class<T> clazz) {
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

    protected void resizeObjectArray() {
        final int currentSize = objectArray.length;
        final int newSize = currentSize << 1;
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
    }
}
