package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import lombok.ToString;

import java.nio.charset.StandardCharsets;

@ToString(callSuper = true)
public class LongField extends AbstractField {

    public static final long DEFAULT_VALUE = Long.MIN_VALUE;
    public static final int FIELD_DATA_LENGTH = 6; // 5 digits plus optional sign
    private long value = DEFAULT_VALUE;

    public LongField(int number) {
        super(number, FIELD_DATA_LENGTH, false);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.LONG;
    }

    public long getValue() {
        if (value == DEFAULT_VALUE) { //I know it's not "thread safe" but this method is supposed to be called by single thread at a time anyway so no need to synchronize
            fieldData.readerIndex(0);
            value = ((AsciiString) fieldData.readCharSequence(fieldData.writerIndex(), StandardCharsets.US_ASCII)).parseLong();
        }
        return value;
    }

    public void setValue(long value) {
        this.value = value;
        final CharSequence charSequence = FieldUtils.toCharSequence(value);
        fieldData.clear().writeCharSequence(charSequence, StandardCharsets.US_ASCII);
        ReferenceCountUtil.release(charSequence);
    }

    @Override
    protected void resetInnerState() {
        value = DEFAULT_VALUE;
    }
}
