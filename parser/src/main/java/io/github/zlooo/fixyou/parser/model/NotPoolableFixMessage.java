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
            int minIndex = Integer.MAX_VALUE;
            for (final AbstractField field : getFieldsOrdered()) {
                if (field.isValueSet()) {
                    final int endIndex = field.getEndIndex();
                    if (maxIndex < endIndex) {
                        maxIndex = endIndex;
                    }
                    final int startIndex = field.getStartIndex();
                    if (minIndex > startIndex) {
                        minIndex = startIndex;
                    }
                }
            }
            messageByteSource.releaseData(minIndex - 2/*-2 because 8=*/, maxIndex);
        }
    }
}
