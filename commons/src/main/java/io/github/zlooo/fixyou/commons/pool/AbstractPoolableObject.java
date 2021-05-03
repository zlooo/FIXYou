package io.github.zlooo.fixyou.commons.pool;

import io.github.zlooo.fixyou.Closeable;
import io.netty.util.ReferenceCounted;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ToString
public abstract class AbstractPoolableObject implements ReferenceCounted, Closeable { //TODO check if extending AbstractReferenceCounted does not give better results

    static final int AVAILABLE_STATE = 0;
    static final int IN_USE_STATE = 1;

    protected boolean exceptionOnReferenceCheckFail = true;
    private final AtomicInteger state = new AtomicInteger(AVAILABLE_STATE);
    private final AtomicInteger referenceCount = new AtomicInteger(0);
    @ToString.Exclude
    private ObjectPool pool;

    @Override
    public int refCnt() {
        return referenceCount.intValue();
    }

    @Override
    public ReferenceCounted retain() {
        referenceCount.incrementAndGet();
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        referenceCount.addAndGet(increment);
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
        return checkReferenceCount(referenceCount.decrementAndGet());
    }

    @Override
    public boolean release(int decrement) {
        return checkReferenceCount(referenceCount.addAndGet(-decrement));
    }

    private boolean checkReferenceCount(int refCount) {
        final int compareResult = Long.compare(refCount, 0L);
        if (compareResult == 0) {
            deallocate();
            return true;
        } else if (compareResult < 0 && exceptionOnReferenceCheckFail) {
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
