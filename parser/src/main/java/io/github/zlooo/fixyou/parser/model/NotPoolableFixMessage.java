package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.commons.memory.Region;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.commons.utils.ReflectionUtils;
import io.github.zlooo.fixyou.utils.UnsafeAccessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class NotPoolableFixMessage extends FixMessage {

    public NotPoolableFixMessage() {
        super(new FakePool());
        exceptionOnReferenceCheckFail = false;
        retain();
    }

    @Override
    protected void deallocate() {
        close();
    }

    //TODO FakePool? That doesn't sound cool at all :/ Maybe FixMessage should not be dealing with pool but some other interface?
    private static class FakePool implements ObjectPool<Region> {

        @Override
        public void close() {
            //nothing to do
        }

        @Nullable
        @Override
        public Region tryGetAndRetain() {
            return new Region(UnsafeAccessor.UNSAFE.allocateMemory(DefaultConfiguration.REGION_SIZE), DefaultConfiguration.REGION_SIZE);
        }

        @Nonnull
        @Override
        public Region getAndRetain() {
            return new Region(UnsafeAccessor.UNSAFE.allocateMemory(DefaultConfiguration.REGION_SIZE), DefaultConfiguration.REGION_SIZE);
        }

        @Override
        public void returnObject(@Nonnull Region objectToBeReturned) {
            if (objectToBeReturned.getStartingAddress() > 0) {
                UnsafeAccessor.UNSAFE.freeMemory(objectToBeReturned.getStartingAddress());
                //TODO yet another reason for getting rid of FakePool
                ReflectionUtils.setFinalField(objectToBeReturned, "startingAddress", -1L);
            }
        }

        @Override
        public boolean areAllObjectsReturned() {
            return true;
        }
    }
}
