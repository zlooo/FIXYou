package io.github.zlooo.fixyou.commons.pool;

import io.github.zlooo.fixyou.Closeable;
import io.netty.util.ReferenceCounted;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@ToString
public abstract class AbstractPoolableObject implements ReferenceCounted, Closeable { //TODO check if extending AbstractReferenceCounted does not give better results

    static final int AVAILABLE_STATE = 0;
    static final int IN_USE_STATE = 1;

    private final AtomicInteger state = new AtomicInteger(AVAILABLE_STATE);
    private final LongAdder referenceCount = new LongAdder();
    private ObjectPool pool;

    @Override
    public int refCnt() {
        return referenceCount.intValue();
    }

    @Override
    public ReferenceCounted retain() {
        referenceCount.increment();
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        referenceCount.add(increment);
        return this;
    }

    @Override
    public ReferenceCounted touch() {
        return touch(null);
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }

    @Override
    public boolean release() {
        referenceCount.decrement();
        return checkReferenceCount();
    }

    @Override
    public boolean release(int decrement) {
        referenceCount.add(-decrement);
        return checkReferenceCount();
    }

    private boolean checkReferenceCount() {
        final int compareResult = Long.compare(referenceCount.sum(), 0L);
        if (compareResult == 0) {
            deallocate();
            return true;
        } else if (compareResult < 0) {
            throw new IllegalStateException("Dude you're releasing object that already has reference count = 0, something is not right with your code");
        }
        return false;
    }

    protected void deallocate() {
        if (pool != null) {
            pool.returnObject(this);
        } else {
            log.warn("Pool for this object is null!!!! This can lead to memory leak. Object details are logged on debug level");
            log.debug("Object details {}", this);
        }
    }

    void setPool(ObjectPool pool) {
        this.pool = pool;
    }

    protected ObjectPool getPool() {
        return this.pool;
    }

    AtomicInteger getState() {
        return state;
    }
}
