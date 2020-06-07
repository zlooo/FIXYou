package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FieldType;
import lombok.ToString;

@ToString(callSuper = true)
public class LongField extends AbstractField {

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

    public long getValue() {
        if (value == DEFAULT_VALUE) {
            fieldData.readerIndex(startIndex);
            value = ParsingUtils.parseLong(fieldData, FixMessage.FIELD_SEPARATOR_BYTE); //TODO run JMH test and see if you're right
        }
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    protected void resetInnerState() {
        value = DEFAULT_VALUE;
    }
}
