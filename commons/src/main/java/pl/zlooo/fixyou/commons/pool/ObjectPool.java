package pl.zlooo.fixyou.commons.pool;

import pl.zlooo.fixyou.Closeable;

public interface ObjectPool<T extends AbstractPoolableObject> extends Closeable {
    T getAndRetain();

    void returnObject(T objectToBeReturned);
}
