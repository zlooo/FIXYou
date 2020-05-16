package pl.zlooo.fixyou.parser.model;

import io.netty.util.ReferenceCounted;
import pl.zlooo.fixyou.model.FixSpec;

public final class NotPoolableFixMessage extends FixMessage {

    public NotPoolableFixMessage(FixSpec spec) {
        super(spec);
    }

    //I know it's a bit shady and it fucks up my feng shui as well, but I can't make class "unextend" an interface :/
    @Override
    public int refCnt() {
        return Integer.MIN_VALUE;
    }

    @Override
    public ReferenceCounted retain() {
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment) {
        return this;
    }

    @Override
    public ReferenceCounted touch() {
        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }

    @Override
    public boolean release() {
        return false;
    }

    @Override
    public boolean release(int decrement) {
        return false;
    }
}
