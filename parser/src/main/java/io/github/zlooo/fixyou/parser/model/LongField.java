package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class LongField extends AbstractField {

    public static final long DEFAULT_VALUE = Long.MIN_VALUE;
    public static final int FIELD_DATA_LENGTH = 8; // 7 digits plus optional sign
    private long value = DEFAULT_VALUE;
    private byte[] rawValue = new byte[FIELD_DATA_LENGTH];
    private char[] unparsedValue = new char[FIELD_DATA_LENGTH];

    public LongField(int number) {
        super(number);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.LONG;
    }

    @Override
    public void appendByteBufWithValue(ByteBuf out) {
        FieldUtils.writeEncoded(value, out);
    }

    public long getValue() {
        if (value == DEFAULT_VALUE && valueSet) {
            fieldData.readerIndex(startIndex);
            value = ParsingUtils.parseLong(fieldData, FixMessage.FIELD_SEPARATOR); //TODO run JMH test and see if you're right
        }
        return value;
    }

    public void setValue(long value) {
        this.value = value;
        this.valueSet = true;
    }

    @Override
    protected void resetInnerState() {
        value = DEFAULT_VALUE;
    }
}
