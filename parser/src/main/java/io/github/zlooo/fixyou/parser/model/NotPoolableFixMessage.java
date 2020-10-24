package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.FixSpec;

import javax.annotation.Nonnull;

public final class NotPoolableFixMessage extends FixMessage {

    public NotPoolableFixMessage(@Nonnull FixSpec spec, @Nonnull FieldCodec fieldCodec) {
        super(spec, fieldCodec);
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
            for (final Field field : getFieldsOrdered()) {
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
