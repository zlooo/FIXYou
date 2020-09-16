package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class LongField extends AbstractField {

    public static final long DEFAULT_VALUE = Long.MIN_VALUE;
    public static final int FIELD_DATA_LENGTH = 8; // 7 digits plus optional sign
    private final ByteBuf rawValue = Unpooled.directBuffer(FIELD_DATA_LENGTH, FIELD_DATA_LENGTH);
    private long value = DEFAULT_VALUE;
    private int sumOfBytes;

    public LongField(int number) {
        super(number);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.LONG;
    }

    @Override
    public int appendByteBufWithValue(ByteBuf out) {
        out.writeBytes(rawValue, 0, rawValue.writerIndex());
        return sumOfBytes;
    }

    public long getValue() {
        if (value == DEFAULT_VALUE && valueSet) {
            value = ParsingUtils.parseLong(fieldData, startIndex, FixMessage.FIELD_SEPARATOR); //TODO run JMH test and see if you're right
        }
        return value;
    }

    public void setValue(long value) {
        this.value = value;
        this.valueSet = true;
        sumOfBytes = FieldUtils.writeEncoded(value, rawValue.clear());
    }

    @Override
    protected void resetInnerState() {
        value = DEFAULT_VALUE;
        rawValue.clear();
        sumOfBytes = 0;
    }
}
