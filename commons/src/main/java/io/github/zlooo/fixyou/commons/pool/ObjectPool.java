package io.github.zlooo.fixyou.commons.pool;

import io.github.zlooo.fixyou.Closeable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ObjectPool<T extends AbstractPoolableObject> extends Closeable {

    /**
     * Tries to get object from a pool. If available get it and increase reference count. If not available just returns null and does <b>NOT</b> resize a pool. That's the main difference between this method and {@link #getAndRetain()}
     */
    @Nullable
    T tryGetAndRetain();

    /**
     * Gets object from a pool and adds 1 to reference count. If pool is empty resizes it and tries again
     */
    @Nonnull
    T getAndRetain();

    void returnObject(@Nonnull T objectToBeReturned);

    boolean areAllObjectsReturned();
}
