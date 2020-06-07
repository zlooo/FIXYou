package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FieldType;
import lombok.ToString;

@ToString(callSuper = true)
public class DoubleField extends AbstractField {

    private static final long DEFAULT_VALUE = Long.MIN_VALUE;
    private static final char FRACTION_SEPARATOR = '.';
    private static final int FIELD_DATA_LENGTH = 17; //15 significant digits, optional sign and optional decimal point
    private final byte[] rawValue = new byte[FIELD_DATA_LENGTH];
    private final char[] unparsedValue = new char[FIELD_DATA_LENGTH];
    private long value = DEFAULT_VALUE;
    private short scale;

    public DoubleField(int number) {
        super(number);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.DOUBLE;
    }

    public long getValue() {
        if (value == DEFAULT_VALUE) {
            decodeValuesFromFieldData();
        }
        return value;
    }

    private void decodeValuesFromFieldData() {
        fieldData.readerIndex(startIndex);
        //TODO after JMH check in LongField is done and you're right, check this one as well
        final int length = endIndexIndex - startIndex;
        ParsingUtils.readChars(fieldData, length, rawValue, unparsedValue);
        final boolean negative = unparsedValue[0] == '-';
        value = 0;
        for (int i = negative ? 1 : 0; i < length; i++) {
            final char nextChar = unparsedValue[i];
            if (nextChar != FRACTION_SEPARATOR) {
                value = value * ParsingUtils.RADIX + ((int) nextChar - ParsingUtils.ASCII_ZERO_CODE);
            } else {
                scale = (short) (length - i - 1);
            }
        }
        if (negative) {
            value *= -1;
        }
    }

    public short getScale() {
        if (value == DEFAULT_VALUE) {
            decodeValuesFromFieldData();
        }
        return scale;
    }

    public void setValue(long newValue, short newScale) {
        this.value = newValue;
        this.scale = newScale;
    }

    @Override
    protected void resetInnerState() {
        value = DEFAULT_VALUE;
        scale = 0;
    }
}
