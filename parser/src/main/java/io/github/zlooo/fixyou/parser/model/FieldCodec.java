package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.ReusableCharArray;
import io.github.zlooo.fixyou.commons.utils.DateUtils;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;
import org.agrona.collections.Hashing;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FieldCodec {

    boolean getBooleanValue(FieldValue fieldValue, Field field) {
        if (!fieldValue.isParsed() && field.isIndicesSet()) {
            final byte byteRead = field.getFieldData().getByte(field.getStartIndex());
            switch (byteRead) {
                case AsciiCodes.Y:
                    fieldValue.setBooleanValue(true);
                    break;
                case AsciiCodes.N:
                    fieldValue.setBooleanValue(false);
                    break;
                default:
                    throw new IllegalArgumentException("Value " + byteRead + " is unsupported in boolean field. Expecting either 'Y' or 'N'");
            }
            fieldValue.setParsed(true);
        }
        return fieldValue.isBooleanValue();
    }

    void setBooleanValue(boolean newValue, FieldValue fieldValue) {
        fieldValue.setBooleanValue(newValue);
        fieldValue.setParsed(true);
        fieldValue.setValueTypeSet(FieldType.BOOLEAN);
    }

    char getCharValue(FieldValue fieldValue, Field field) {
        if (fieldValue.getCharValue() == FieldValue.CHAR_DEFAULT_VALUE && field.isIndicesSet()) {
            field.setCharValue(AsciiString.b2c(field.getFieldData().getByte(field.getStartIndex())));
        }
        return fieldValue.getCharValue();
    }

    void setCharValue(char newValue, FieldValue fieldValue) {
        fieldValue.setCharValue(newValue);
        fieldValue.setValueTypeSet(FieldType.CHAR);
    }

    MutableCharSequence getCharSequenceValue(FieldValue fieldValue, Field field) {
        if (fieldValue.getLength() == 0 && field.isIndicesSet()) {
            fieldValue.setLength(field.getLength());
            ensureSufficientTablesLength(fieldValue, fieldValue.getLength());
            ParsingUtils.readChars(field.getFieldData(), field.getStartIndex(), fieldValue.getLength(), fieldValue.getRawValue(), fieldValue.getCharArrayValue());
        }
        return fieldValue.getCharSequenceValue();
    }

    private void ensureSufficientTablesLength(FieldValue fieldValue, int length) {
        if (fieldValue.getCharArrayValue().length < length) {
            final int newTableLength = (int) ((length + 1) * (1.0 + Hashing.DEFAULT_LOAD_FACTOR));
            fieldValue.getRawValue().ensureWritable(newTableLength);
            final char[] charArrayValue = new char[newTableLength];
            fieldValue.setCharArrayValue(charArrayValue);
            fieldValue.getCharSequenceValue().setState(charArrayValue);
        }
    }

    void setCharSequenceValue(char[] newValue, int newValueLength, FieldValue fieldValue) {
        fieldValue.setLength(newValueLength);
        final ByteBuf rawValue = fieldValue.getRawValue().clear();
        ensureSufficientTablesLength(fieldValue, newValueLength);
        final char[] charArrayValue = fieldValue.getCharArrayValue();
        System.arraycopy(newValue, 0, charArrayValue, 0, newValueLength);
        int sumOfBytes = 0;
        for (int i = 0; i < newValueLength; i++) {
            final byte byteToWrite = AsciiString.c2b(ArrayUtils.getElementAt(charArrayValue, i));
            sumOfBytes += byteToWrite;
            rawValue.setByte(i, byteToWrite);
        }
        rawValue.writerIndex(newValueLength);
        fieldValue.setSumOfBytes(sumOfBytes);
        fieldValue.setValueTypeSet(FieldType.CHAR_ARRAY);
    }

    long getDoubleUnscaledValue(FieldValue fieldValue, Field field) {
        if (fieldValue.getLongValue() == FieldValue.LONG_DEFAULT_VALUE && field.isIndicesSet()) {
            decodeDoubleValuesFromFieldData(fieldValue, field);
        }
        return fieldValue.getLongValue();
    }

    short getScale(FieldValue fieldValue, Field field) {
        if (fieldValue.getLongValue() == FieldValue.LONG_DEFAULT_VALUE && field.isIndicesSet()) {
            decodeDoubleValuesFromFieldData(fieldValue, field);
        }
        return fieldValue.getScale();
    }

    private void decodeDoubleValuesFromFieldData(FieldValue fieldValue, Field field) {
        //TODO after JMH check in LongField is done and you're right, check this one as well
        final int length = field.getLength();
        fieldValue.setLength(length);
        final char[] charArrayValue = fieldValue.getCharArrayValue();
        ParsingUtils.readChars(field.getFieldData(), field.getStartIndex(), length, fieldValue.getRawValue(), charArrayValue);
        final boolean negative = charArrayValue[0] == '-';
        long longValue = 0;
        for (int i = negative ? 1 : 0; i < length; i++) {
            final char nextChar = charArrayValue[i];
            if (nextChar != ParsingUtils.FRACTION_SEPARATOR) {
                longValue = longValue * ParsingUtils.RADIX + ((int) nextChar - AsciiCodes.ZERO);
            } else {
                fieldValue.setScale((short) (length - i - 1));
            }
        }
        if (negative) {
            longValue *= -1;
        }
        fieldValue.setLongValue(longValue);
    }

    void setDoubleValue(long newValue, short newScale, FieldValue fieldValue) {
        fieldValue.setLongValue(newValue);
        fieldValue.setScale(newScale);
        //TODO refactor this so that value is written directly, not converted to char[] first
        final ReusableCharArray valueAsChar = FieldUtils.toCharSequence(newValue, 1);
        final int length = valueAsChar.length();
        final int separatorIndex = length - newScale - 1;
        final char[] valueAsCharArray = valueAsChar.getCharArray();
        ArrayUtils.insertElementAtIndex(valueAsCharArray, ParsingUtils.FRACTION_SEPARATOR, separatorIndex);
        int sumOfBytes = 0;
        final ByteBuf rawValue = fieldValue.getRawValue().clear();
        for (int i = 0; i < length; i++) {
            final byte byteToWrite = AsciiString.c2b(ArrayUtils.getElementAt(valueAsCharArray, i));
            sumOfBytes += byteToWrite;
            rawValue.setByte(i, byteToWrite);
        }
        rawValue.writerIndex(length);
        fieldValue.setLength(length);
        fieldValue.setSumOfBytes(sumOfBytes);
        ReferenceCountUtil.release(valueAsChar);
        fieldValue.setValueTypeSet(FieldType.DOUBLE);
    }

    long getLongValue(FieldValue fieldValue, Field field) {
        if (fieldValue.getLongValue() == FieldValue.LONG_DEFAULT_VALUE && field.isIndicesSet()) {
            fieldValue.setLongValue(ParsingUtils.parseLong(field.getFieldData(), field.getStartIndex(), FixMessage.FIELD_SEPARATOR)); //TODO run JMH test and see if you're right
        }
        return fieldValue.getLongValue();
    }

    void setLongValue(long newValue, FieldValue fieldValue) {
        fieldValue.setLongValue(newValue);
        final ByteBuf rawValue = fieldValue.getRawValue().clear();
        fieldValue.setSumOfBytes(FieldUtils.writeEncoded(newValue, rawValue));
        fieldValue.setLength(rawValue.writerIndex());
        fieldValue.setValueTypeSet(FieldType.LONG);
    }

    long getTimestampValue(FieldValue fieldValue, Field field) {
        if (fieldValue.getLongValue() == FieldValue.LONG_DEFAULT_VALUE && field.isIndicesSet()) { //TODO seriously? write a method that parses directly to epoch timestamp you lazy son of a bitch
            //TODO it's last place that uses formatter get rid of it when you refactor this code
            final int length = field.getLength();
            ParsingUtils.readChars(field.getFieldData(), field.getStartIndex(), length, fieldValue.getRawValue(), fieldValue.getCharArrayValue());
            fieldValue.setLength(length);
            fieldValue.setLongValue(ParsingUtils.chooseFormatter(length).parse(fieldValue.getCharSequenceValue(), LocalDateTime::from).toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        return fieldValue.getLongValue();
    }

    void setTimestampValue(long newValue, FieldValue fieldValue) {
        fieldValue.setLongValue(newValue);
        final ByteBuf rawValue = fieldValue.getRawValue().clear();
        fieldValue.setSumOfBytes(DateUtils.writeTimestamp(newValue, rawValue, true));
        fieldValue.setLength(rawValue.writerIndex());
        fieldValue.setValueTypeSet(FieldType.TIMESTAMP);
    }

    int appendByteBufWithValue(ByteBuf out, FieldValue fieldValue, Field field, FixSpec fixSpec) {
        final int returnValue;
        switch (fieldValue.getValueTypeSet()) {
            case BOOLEAN:
                returnValue = appendByteBufWithBooleanValue(out, fieldValue.isBooleanValue());
                break;
            case CHAR:
                returnValue = appendByteBufWithCharValue(out, fieldValue.getCharValueRaw());
                break;
            case LONG:
            case DOUBLE:
            case TIMESTAMP:
            case CHAR_ARRAY:
                returnValue = appendByteBufWithBytesValue(out, fieldValue);
                break;
            case GROUP:
                returnValue = appendByteBufWithGroupValue(out, fieldValue, field, fixSpec);
                break;
            default:
                throw new FieldValueNotSetException(field);
        }
        return returnValue;
    }

    private int appendByteBufWithBooleanValue(ByteBuf out, boolean booleanValue) {
        if (booleanValue) {
            out.writeByte(AsciiCodes.Y);
            return AsciiCodes.Y;
        } else {
            out.writeByte(AsciiCodes.N);
            return AsciiCodes.N;
        }
    }

    private int appendByteBufWithCharValue(ByteBuf out, int charValueRaw) {
        out.writeByte(charValueRaw);
        return charValueRaw;
    }

    private int appendByteBufWithBytesValue(ByteBuf out, FieldValue fieldValue) {
        out.writeBytes(fieldValue.getRawValue(), 0, fieldValue.getLength());
        return fieldValue.getSumOfBytes();
    }

    private int appendByteBufWithGroupValue(ByteBuf out, FieldValue fieldValue, Field field, FixSpec fixSpec) {
        final int repetitionCounter = fieldValue.getRepetitionCounter();
        int sumOfBytes = FieldUtils.writeEncoded(repetitionCounter, out) + FixMessage.FIELD_SEPARATOR;
        out.writeByte(FixMessage.FIELD_SEPARATOR);
        final int[] groupFieldsOrder = fixSpec.getRepeatingGroupFieldNumbers(field.getNumber());
        for (int i = 0; i < repetitionCounter; i++) {
            final FieldValue.Repetition repetition = ArrayUtils.getElementAt(fieldValue.getRepetitions(), i);
            for (int j = 0; j < groupFieldsOrder.length; j++) {
                final Field repetitionField = repetition.getExistingGroupFieldOrNull(ArrayUtils.getElementAt(groupFieldsOrder, j));
                if (repetitionField != null && repetitionField.isValueSet()) {
                    sumOfBytes += repetitionField.appendFieldNumber(out);
                    sumOfBytes += repetitionField.appendByteBufWithValue(out, fixSpec) + FixMessage.FIELD_SEPARATOR;
                    out.writeByte(FixMessage.FIELD_SEPARATOR);
                }
            }
        }
        out.writerIndex(out.writerIndex() - 1); //"remove" last soh from buffer
        return sumOfBytes - FixMessage.FIELD_SEPARATOR;
    }
}
