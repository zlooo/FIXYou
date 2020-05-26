package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.ReusableCharArray;
import io.github.zlooo.fixyou.commons.utils.ArrayUtils;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import lombok.ToString;

import java.nio.charset.StandardCharsets;

@ToString(callSuper = true)
public class DoubleField extends AbstractField {

    private static final long DEFAULT_VALUE = Long.MIN_VALUE;
    private static final char FRACTION_SEPARATOR = '.';
    private static final int RADIX = 10;
    private static final int ASCII_ZERO_CODE = 48;
    private static final int FIELD_DATA_LENGTH = 17; //15 significant digits, optional sign and optional decimal point
    private long value = DEFAULT_VALUE;
    private short scale;

    public DoubleField(int number) {
        super(number, FIELD_DATA_LENGTH, false);
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
        fieldData.readerIndex(0);
        final char[] rawValue = ((AsciiString) fieldData.readCharSequence(fieldData.writerIndex(), StandardCharsets.US_ASCII)).toCharArray();
        final boolean negative = rawValue[0] == '-';
        value = 0;
        final int length = rawValue.length;
        for (int i = negative ? 1 : 0; i < length; i++) {
            final char nextChar = rawValue[i];
            if (nextChar != FRACTION_SEPARATOR) {
                value = value * RADIX + ((int) nextChar - ASCII_ZERO_CODE);
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
        final ReusableCharArray valueAsChar = FieldUtils.toCharSequence(newValue, 1);
        fieldData.clear();
        final int separatorIndex = valueAsChar.length() - newScale - 1;
        ArrayUtils.insertElementAtIndex(valueAsChar.getCharArray(), FRACTION_SEPARATOR, separatorIndex);
        fieldData.writeCharSequence(valueAsChar, StandardCharsets.US_ASCII);
        ReferenceCountUtil.release(valueAsChar);
    }

    @Override
    protected void resetInnerState() {
        value = DEFAULT_VALUE;
        scale = 0;
    }
}
