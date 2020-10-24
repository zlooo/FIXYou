package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.internal.PlatformDependent;
import lombok.*;
import org.agrona.collections.Hashing;
import org.agrona.collections.Int2ObjectHashMap;

@EqualsAndHashCode
@ToString(exclude = {"encodedFieldNumber", "fieldData", "fieldCodec"})
public class Field implements Closeable {

    @Getter
    private final int number;
    @Setter(AccessLevel.PROTECTED)
    @Getter(AccessLevel.PROTECTED)
    private ByteBufComposer fieldData;
    @Getter
    private int startIndex;
    @Getter
    private int endIndex;
    @Getter(AccessLevel.PACKAGE)
    private boolean indicesSet;
    @Getter(value = AccessLevel.PROTECTED)
    private final byte[] encodedFieldNumber;
    private final int encodedFieldNumberSumOfBytes;
    @Getter
    private final int encodedFieldNumberLength;
    private final FieldValue fieldValue;
    private final FieldCodec fieldCodec;

    public Field(int number, FixSpec fixSpec, FieldCodec fieldCodec) {
        this.number = number;
        this.fieldCodec = fieldCodec;
        this.fieldValue = new FieldValue(() -> {
            final int[] childFieldNumbers = fixSpec.getRepeatingGroupFieldNumbers(number);
            final int numberOfChildFields = childFieldNumbers.length;
            final Int2ObjectHashMap<Field> idToFieldMap = new Int2ObjectHashMap<>(numberOfChildFields, Hashing.DEFAULT_LOAD_FACTOR);
            final Field[] fieldsOrdered = new Field[numberOfChildFields];
            for (int i = 0; i < numberOfChildFields; i++) {
                final int fieldNumber = ArrayUtils.getElementAt(childFieldNumbers, i);
                final Field field = new Field(fieldNumber, fixSpec, fieldCodec);
                idToFieldMap.put(fieldNumber, field);
                fieldsOrdered[i] = field;
                if (fieldData != null) {
                    field.setFieldData(fieldData);
                }
            }
            return new FieldValue.Repetition(fieldsOrdered, idToFieldMap);
        });
        final char[] fieldNumberAsCharArray = Integer.toString(number).toCharArray(); //we're doing it just on startup so we can afford it
        final int encodedFieldNumberCapacity = fieldNumberAsCharArray.length + 1;
        encodedFieldNumber = new byte[encodedFieldNumberCapacity];
        int tempSumOfBytes = 0;
        for (int i = 0; i < fieldNumberAsCharArray.length; i++) {
            final byte encodedChar = AsciiString.c2b(fieldNumberAsCharArray[i]);
            tempSumOfBytes += encodedChar;
            PlatformDependent.putByte(encodedFieldNumber, i, encodedChar);
        }
        tempSumOfBytes += FixMessage.FIELD_VALUE_SEPARATOR;
        PlatformDependent.putByte(encodedFieldNumber, encodedFieldNumberCapacity - 1, FixMessage.FIELD_VALUE_SEPARATOR);
        encodedFieldNumberSumOfBytes = tempSumOfBytes;
        encodedFieldNumberLength = encodedFieldNumber.length;

    }

    public void setIndexes(int newStartIndex, int newEndIndexIndex) {
        this.startIndex = newStartIndex;
        this.endIndex = newEndIndexIndex;
        this.indicesSet = true;
    }

    public boolean isValueSet() {
        return indicesSet || fieldValue.getValueTypeSet() != null;
    }

    public int getLength() {
        if (indicesSet) {
            return endIndex - startIndex;
        } else {
            return fieldValue.getLength();
        }
    }

    public void reset() {
        if (isValueSet()) {
            fieldValue.reset();
        }
        this.startIndex = 0;
        this.endIndex = 0;
        this.indicesSet = false;
    }

    @Override
    public void close() {
        fieldValue.close();
    }

    public int appendFieldNumber(ByteBuf out) {
        out.writeBytes(encodedFieldNumber);
        return encodedFieldNumberSumOfBytes;
    }

    public boolean getBooleanValue() {
        return fieldCodec.getBooleanValue(fieldValue, this);
    }

    public void setBooleanValue(boolean newValue) {
        fieldCodec.setBooleanValue(newValue, fieldValue);
    }

    public char getCharValue() {
        return fieldCodec.getCharValue(fieldValue, this);
    }

    public void setCharValue(char newValue) {
        fieldCodec.setCharValue(newValue, fieldValue);
    }

    public CharSequence getCharSequenceValue() {
        return fieldCodec.getCharSequenceValue(fieldValue, this);
    }

    public char[] getCharArrayValue() {
        return fieldCodec.getCharSequenceValue(fieldValue, this).getState();
    }

    public void setCharSequenceValue(char[] newValue) {
        setCharSequenceValue(newValue, newValue.length);
    }

    public void setCharSequenceValue(Field sourceField) {
        setCharSequenceValue(sourceField.fieldValue.getCharArrayValue(), sourceField.getLength());
    }

    public void setCharSequenceValue(char[] newValue, int newValueLength) {
        this.fieldCodec.setCharSequenceValue(newValue, newValueLength, fieldValue);
    }

    public long getDoubleUnscaledValue() {
        return fieldCodec.getDoubleUnscaledValue(fieldValue, this);
    }

    public short getScale() {
        return fieldCodec.getScale(fieldValue, this);
    }

    public void setDoubleValue(long newValue, short newScale) {
        this.fieldCodec.setDoubleValue(newValue, newScale, fieldValue);
    }

    public long getLongValue() {
        return fieldCodec.getLongValue(fieldValue, this);
    }

    public void setLongValue(long newValue) {
        this.fieldCodec.setLongValue(newValue, fieldValue);
    }

    public long getTimestampValue() {
        return fieldCodec.getTimestampValue(fieldValue, this);
    }

    public void setTimestampValue(long newValue) {
        this.fieldCodec.setTimestampValue(newValue, fieldValue);
    }

    public int appendByteBufWithValue(ByteBuf out) {
        return fieldCodec.appendByteBufWithValue(out, fieldValue, this);
    }

    public Field getFieldForCurrentRepetition(int fieldNum) {
        return fieldValue.getCurrentRepetition().getIdToField().get(fieldNum);
    }

    public Field getFieldForGivenRepetition(int repetitionIndex, int fieldNum) {
        return ArrayUtils.getElementAt(fieldValue.getRepetitions(), repetitionIndex).getIdToField().get(fieldNum);
    }

    public Field endCurrentRepetition() {
        fieldValue.setValueTypeSet(FieldType.GROUP);
        fieldValue.incrementRepetitionsCounter();
        return this;
    }
}
