package io.github.zlooo.fixyou.commons.memory;

import io.github.zlooo.fixyou.commons.pool.ArrayBackedObjectPool;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.utils.UnsafeAccessor;
import lombok.ToString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

@ToString
public class RegionPool implements ObjectPool<Region> {

    private final ObjectPool<Region> delegate;
    private final long poolAddress;

    public RegionPool(int regionCount, short regionSize) {
        poolAddress = UnsafeAccessor.UNSAFE.allocateMemory((long) regionCount * regionSize);
        final AtomicInteger objectCounter = new AtomicInteger();
        delegate = new ArrayBackedObjectPool<>(regionCount, () -> new Region(poolAddress + (regionSize * objectCounter.getAndIncrement()), regionSize), Region.class, regionCount);
    }

    @Override
    public void close() {
        UnsafeAccessor.UNSAFE.freeMemory(poolAddress);
    }

    @Nullable
    @Override
    public Region tryGetAndRetain() {
        return delegate.tryGetAndRetain();
    }

    @Nonnull
    @Override
    public Region getAndRetain() {
        throw new UnsupportedOperationException("This class does not support resize, please use tryGetAndRetain instead");
    }

    @Override
    public void returnObject(@Nonnull Region objectToBeReturned) {
        delegate.returnObject(objectToBeReturned);
    }

    @Override
    public boolean areAllObjectsReturned() {
        return delegate.areAllObjectsReturned();
    }
}
