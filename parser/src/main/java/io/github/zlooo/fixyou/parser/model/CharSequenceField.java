package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FieldType;
import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Hashing;

import java.nio.charset.StandardCharsets;

@Slf4j
@EqualsAndHashCode(callSuper = true, exclude = {"value"})
@ToString(callSuper = true)
public final class CharSequenceField extends AbstractField {

    private static final int STARTING_LENGTH = 10;

    private final MutableCharSequence returnValue = new MutableCharSequence();
    private int length;
    private byte[] rawValue;
    private char[] value;

    public CharSequenceField(int number) {
        super(number);
        rawValue = new byte[STARTING_LENGTH];
        value = new char[STARTING_LENGTH];
        returnValue.setState(value);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.CHAR_ARRAY;
    }

    @Override
    public void appendByteBufWithValue(ByteBuf out) {
        out.writeCharSequence(returnValue, StandardCharsets.US_ASCII);
    }

    public CharSequence getValue() {
        if (length == 0 && valueSet) {
            length = endIndex - startIndex;
            ensureSufficientTablesLength();
            ParsingUtils.readChars(fieldData, startIndex, length, rawValue, value);
            returnValue.setLength(length);
        }
        return returnValue;
    }

    /**
     * <B>Warning, use with care</B><br>
     * Be mindful that this method returns an array that's actually used to store parsed value. This means two things:
     * <ol>
     *     <li>if you change content of this array you'll practically speaking "change the value of this field" since result of {@link #getValue()} also uses this array</li>
     *     <li>this array's length may be, and in most cases will be, larger than the amount of chars needed to represent this field's value, use {@link #getLength()} to check "real length" instead</li>
     * </ol>
     */
    public char[] getUnderlyingValue() {
        return value;
    }

    private void ensureSufficientTablesLength() {
        if (rawValue.length < length) {
            final int newTableLength = (int) ((length + 1) * (1.0 + Hashing.DEFAULT_LOAD_FACTOR));
            rawValue = new byte[newTableLength];
            value = new char[newTableLength];
            returnValue.setState(value);
        }
    }

    public void setValue(char[] value) {
        length = value.length;
        returnValue.setLength(length);
        ensureSufficientTablesLength();
        System.arraycopy(value, 0, this.value, 0, length);
        this.valueSet = true;
    }

    public void setValue(CharSequenceField sourceValue) {
        length = sourceValue.length;
        returnValue.setLength(length);
        ensureSufficientTablesLength();
        System.arraycopy(sourceValue.value, 0, this.value, 0, length);
        this.valueSet = true;
    }

    public void setValue(char[] newValue, int valueLength) {
        length = valueLength;
        returnValue.setLength(length);
        ensureSufficientTablesLength();
        System.arraycopy(newValue, 0, this.value, 0, length);
        this.valueSet = true;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    protected void resetInnerState() {
        length = 0;
        returnValue.setLength(length);
    }
}
