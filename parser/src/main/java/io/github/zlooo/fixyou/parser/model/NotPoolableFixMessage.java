package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FixSpec;

public final class NotPoolableFixMessage extends FixMessage {

    public NotPoolableFixMessage(FixSpec spec) {
        super(spec);
        exceptionOnReferenceCheckFail = false;
        retain();
    }

    @Override
    protected void deallocate() {
        close();
    }
}
