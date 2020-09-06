package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.ReusableCharArray;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.PlatformDependent;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class DoubleField extends AbstractField {

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
        if (value == DEFAULT_VALUE && valueSet) {
            decodeValuesFromFieldData();
        }
        return value;
    }

    private void decodeValuesFromFieldData() {
        //TODO after JMH check in LongField is done and you're right, check this one as well
        final int length = endIndex - startIndex;
        ParsingUtils.readChars(fieldData, startIndex, length, rawValue, unparsedValue);
        final boolean negative = unparsedValue[0] == '-';
        value = 0;
        for (int i = negative ? 1 : 0; i < length; i++) {
            final char nextChar = unparsedValue[i];
            if (nextChar != FRACTION_SEPARATOR) {
                value = value * ParsingUtils.RADIX + ((int) nextChar - AsciiCodes.ZERO);
            } else {
                scale = (short) (length - i - 1);
            }
        }
        if (negative) {
            value *= -1;
        }
    }

    @Override
    public int appendByteBufWithValue(ByteBuf out) {
        //TODO refactor this so taht value is written directly, not converted to char[] first
        final ReusableCharArray valueAsChar = FieldUtils.toCharSequence(value, 1);
        final int separatorIndex = valueAsChar.length() - scale - 1;
        final char[] valueAsCharArray = valueAsChar.getCharArray();
        ArrayUtils.insertElementAtIndex(valueAsCharArray, FRACTION_SEPARATOR, separatorIndex);
        final byte[] bytesToWrite = new byte[valueAsChar.length()];
        int sumOfBytes = 0;
        for (int i = 0; i < bytesToWrite.length; i++) {
            final byte byteToWrite = AsciiString.c2b(valueAsCharArray[i]);
            sumOfBytes += byteToWrite;
            PlatformDependent.putByte(bytesToWrite, i, byteToWrite);
        }
        out.writeBytes(bytesToWrite);
        ReferenceCountUtil.release(valueAsChar);
        return sumOfBytes;
    }

    public short getScale() {
        if (value == DEFAULT_VALUE && valueSet) {
            decodeValuesFromFieldData();
        }
        return scale;
    }

    public void setValue(long newValue, short newScale) {
        this.value = newValue;
        this.scale = newScale;
        this.valueSet = true;
    }

    @Override
    protected void resetInnerState() {
        value = DEFAULT_VALUE;
        scale = 0;
    }
}
