package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.pool.AbstractPoolableObject;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.utils.FieldTypeUtils;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

@Getter
public class FixMessage extends AbstractPoolableObject {

    public static final byte FIELD_SEPARATOR_BYTE = 0x01;
    public static final char FIELD_SEPARATOR_CHAR = 0x01;
    static final char FIELD_VALUE_SEPARATOR = '=';
    private static final char FIELD_DELIMITER = '|';
    //    private static final String SOH = "\u0001";
    private static final int LONG_MESSAGE_FIELD_NUMBER_THRESHOLD = 10;

    private final AbstractField[] fieldsOrdered;
    private final AbstractField[] fields;
    private ByteBuf messageByteSource;

    public FixMessage(@Nonnull FixSpec spec) {
        final int[] fieldsOrder = spec.getFieldsOrder();
        final FieldType[] fieldTypes = spec.getTypes();
        fieldsOrdered = new AbstractField[fieldsOrder.length];
        fields = new AbstractField[spec.highestFieldNumber() + 1];
        for (int i = 0; i < fieldsOrder.length; i++) {
            final int fieldNumber = fieldsOrder[i];
            final FieldType fieldType = fieldTypes[i];
            final AbstractField field = FieldTypeUtils.createField(fieldType, fieldNumber, spec);
            fieldsOrdered[i] = field;
            fields[fieldNumber] = field;
        }
    }

    public void setMessageByteSource(@Nonnull ByteBuf messageByteSource) {
        messageByteSource.retain();
        this.messageByteSource = messageByteSource;
        for (final AbstractField abstractField : fieldsOrdered) {
            abstractField.setFieldData(messageByteSource);
        }
    }

    @Nullable
    public <T extends AbstractField> T getField(int number) {
        return (T) fields[number];
    }

    /**
     * Call only for debug purposes, NOT on production. Why? Take a look at how it's implemented ;)
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("FixMessage -> ");
        final boolean longMessage = fieldsOrdered.length > LONG_MESSAGE_FIELD_NUMBER_THRESHOLD;
        if (longMessage) {
            for (int i = 0; i < LONG_MESSAGE_FIELD_NUMBER_THRESHOLD; i++) {
                final AbstractField field = fieldsOrdered[i];
                builder.append(field.number).append(FIELD_VALUE_SEPARATOR).append(field.fieldData.toString(StandardCharsets.US_ASCII)).append(FIELD_DELIMITER);
            }
        } else {
            for (final AbstractField field : fieldsOrdered) {
                builder.append(field.number).append(FIELD_VALUE_SEPARATOR).append(field.fieldData.toString(StandardCharsets.US_ASCII)).append(FIELD_DELIMITER);
            }
        }
        builder.deleteCharAt(builder.length() - 1).append(longMessage ? "..." : "").append(", refCnt=").append(refCnt());
        return builder.toString();
    }

    public void resetAllDataFields() {
        for (final AbstractField field : fieldsOrdered) {
                field.reset();
        }
    }

    public void resetDataFields(int... excludes) {
        fieldLoop:
        for (final AbstractField field : fieldsOrdered) {
            final int fieldNumber = field.getNumber();
            for (final int exclude : excludes) {
                if (exclude == fieldNumber) {
                    continue fieldLoop;
                }
            }
            field.reset();
        }
    }

    @Override
    protected void deallocate() {
        resetAllDataFields();
        messageByteSource.release();
        super.deallocate();
    }

    @Override
    public void close() {
        for (final AbstractField field : fields) {
            if (field != null) {
                field.close();
            }
        }
    }

    @Nonnull
    public AbstractField[] getFieldsOrdered() {
        return fieldsOrdered;
    }
}
