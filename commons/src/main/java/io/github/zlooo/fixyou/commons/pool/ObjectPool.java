package io.github.zlooo.fixyou.commons.pool;

import io.github.zlooo.fixyou.Closeable;

public interface ObjectPool<T extends AbstractPoolableObject> extends Closeable {
    T getAndRetain();

    void returnObject(T objectToBeReturned);

    boolean areAllObjectsReturned();
}
