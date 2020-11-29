package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.buffer.ByteBuf;
import lombok.*;

import java.util.concurrent.Executor;

@EqualsAndHashCode
@ToString(exclude = {"fieldData", "fieldCodec"})
public class Field implements Closeable, BooleanField, CharField, CharSequenceField, DoubleField, LongField, TimestampField, GroupField {

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
    private final FieldValue fieldValue;
    private final FieldCodec fieldCodec;

    public Field(int number, FieldCodec fieldCodec) {
        this.number = number;
        this.fieldCodec = fieldCodec;
        this.fieldValue = createFieldValue();
    }

    protected FieldValue createFieldValue() {
        return new FieldValue();
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

    @Override
    public boolean getBooleanValue() {
        return fieldCodec.getBooleanValue(fieldValue, this);
    }

    @Override
    public void setBooleanValue(boolean newValue) {
        fieldCodec.setBooleanValue(newValue, fieldValue);
    }

    @Override
    public char getCharValue() {
        return fieldCodec.getCharValue(fieldValue, this);
    }

    @Override
    public void setCharValue(char newValue) {
        fieldCodec.setCharValue(newValue, fieldValue);
    }

    @Override
    public CharSequence getCharSequenceValue() {
        return fieldCodec.getCharSequenceValue(fieldValue, this);
    }

    @Override
    public char[] getCharArrayValue() {
        return fieldCodec.getCharSequenceValue(fieldValue, this).getState();
    }

    @Override
    public void setCharSequenceValue(char[] newValue) {
        setCharSequenceValue(newValue, newValue.length);
    }

    @Override
    public void setCharSequenceValue(Field sourceField) {
        setCharSequenceValue(sourceField.fieldValue.getCharArrayValue(), sourceField.getLength());
    }

    @Override
    public void setCharSequenceValue(char[] newValue, int newValueLength) {
        this.fieldCodec.setCharSequenceValue(newValue, newValueLength, fieldValue);
    }

    @Override
    public long getDoubleUnscaledValue() {
        return fieldCodec.getDoubleUnscaledValue(fieldValue, this);
    }

    @Override
    public short getScale() {
        return fieldCodec.getScale(fieldValue, this);
    }

    @Override
    public void setDoubleValue(long newValue, short newScale) {
        this.fieldCodec.setDoubleValue(newValue, newScale, fieldValue);
    }

    @Override
    public long getLongValue() {
        return fieldCodec.getLongValue(fieldValue, this);
    }

    @Override
    public void setLongValue(long newValue) {
        this.fieldCodec.setLongValue(newValue, fieldValue);
    }

    @Override
    public long getTimestampValue() {
        return fieldCodec.getTimestampValue(fieldValue, this);
    }

    @Override
    public void setTimestampValue(long newValue) {
        this.fieldCodec.setTimestampValue(newValue, fieldValue);
    }

    /**
     * Encodes data of this field and appends it to give buffer. In case of group field(fix repeating group) provided Executor could be used to populate {@link io.github.zlooo.fixyou.parser.cache.FieldNumberCache}
     * @param out buffer that will receive encoded data
     * @param fixSpec spec used by current session
     * @param executor executor that might be used to populate {@link io.github.zlooo.fixyou.parser.cache.FieldNumberCache} if some field number is not there
     * @return
     */
    public int appendByteBufWithValue(ByteBuf out, FixSpec fixSpec, Executor executor) {
        return fieldCodec.appendByteBufWithValue(out, fieldValue, this, fixSpec, executor);
    }

    @Override
    public Field getFieldForCurrentRepetition(int fieldNum) {
        final Field groupField = fieldValue.getCurrentRepetition().getExistingOrNewGroupField(fieldNum, fieldCodec);
        groupField.setFieldData(fieldData);
        return groupField;
    }

    @Override
    public Field getFieldForGivenRepetition(int repetitionIndex, int fieldNum) {
        final Field groupField = ArrayUtils.getElementAt(fieldValue.getRepetitions(), repetitionIndex).getExistingOrNewGroupField(fieldNum, fieldCodec);
        groupField.setFieldData(fieldData);
        return groupField;
    }

    @Override
    public Field endCurrentRepetition() {
        fieldValue.setValueTypeSet(FieldType.GROUP);
        fieldValue.incrementRepetitionsCounter();
        return this;
    }

    protected void copyDataFrom(Field field) {
        fieldData = field.fieldData;
        if (field.indicesSet) {
            indicesSet = true;
            startIndex = field.startIndex;
            endIndex = field.endIndex;
        }
        fieldValue.copyDataFrom(field.fieldValue, fieldCodec);
    }
}
