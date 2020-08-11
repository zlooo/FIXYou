package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
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
        final ByteBufComposer messageByteSource = getMessageByteSource();
        if (messageByteSource != null) {
            int maxIndex = 0;
            for (final AbstractField field : getFieldsOrdered()) {
                final int endIndex = field.getEndIndex();
                if (maxIndex < endIndex) {
                    maxIndex = endIndex;
                }
            }
            messageByteSource.releaseDataUpTo(maxIndex);
        }
    }
}
