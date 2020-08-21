package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.commons.pool.AbstractPoolableObject;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.utils.FieldTypeUtils;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

@Getter
public class FixMessage extends AbstractPoolableObject {

    public static final byte FIELD_SEPARATOR = AsciiCodes.SOH;
    public static final byte FIELD_VALUE_SEPARATOR = AsciiCodes.EQUALS;
    private static final char FIELD_DELIMITER = '|';
    private static final int LONG_MESSAGE_FIELD_NUMBER_THRESHOLD = 10;

    private final AbstractField[] fieldsOrdered;
    private final AbstractField[] fields;
    private ByteBufComposer messageByteSource;
    @Setter
    private int startIndex;
    @Setter
    private int endIndex;

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

    public void setMessageByteSource(ByteBufComposer newMessageByteSource) {
        this.messageByteSource = newMessageByteSource;
        for (final AbstractField abstractField : fieldsOrdered) {
            abstractField.setFieldData(newMessageByteSource);
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
        return toString(false);
    }

    public String toString(boolean wholeMessage) {
        final StringBuilder builder = new StringBuilder("FixMessage -> ");
        final boolean longMessage = fieldsOrdered.length > LONG_MESSAGE_FIELD_NUMBER_THRESHOLD;
        final boolean shortenOutput = longMessage && !wholeMessage;
        if (shortenOutput) {
            for (int i = 0; i < LONG_MESSAGE_FIELD_NUMBER_THRESHOLD; i++) {
                final AbstractField field = fieldsOrdered[i];
                appendFieldToBuilderIfValueIsSet(builder, field);
            }
        } else {
            for (final AbstractField field : fieldsOrdered) {
                appendFieldToBuilderIfValueIsSet(builder, field);
            }
        }
        builder.deleteCharAt(builder.length() - 1).append(shortenOutput ? "..." : "").append(", refCnt=").append(refCnt());
        return builder.toString();
    }

    private static void appendFieldToBuilderIfValueIsSet(StringBuilder builder, AbstractField field) {
        if (field.isValueSet()) {
            builder.append(field.number).append((char) FIELD_VALUE_SEPARATOR).append(fieldDataValue(field)).append(FIELD_DELIMITER);
        }
    }

    private static String fieldDataValue(AbstractField field) {
        final ByteBufComposer fieldData = field.fieldData;
        if (fieldData != null) {
            final byte[] buf = new byte[field.getLength()];
            fieldData.getBytes(field.startIndex, field.getLength(), buf);
            return new String(buf, StandardCharsets.US_ASCII);
        } else {
            return "";
        }
    }

    public void resetAllDataFieldsAndReleaseByteSource() {
        for (final AbstractField field : fieldsOrdered) {
            if (field.isValueSet()) {
                field.reset();
            }
        }
        if (messageByteSource != null) {
            messageByteSource.releaseData(startIndex, endIndex);
        }
        setMessageByteSource(null);
        startIndex = 0;
        endIndex = 0;
    }

    @Override
    protected void deallocate() {
        resetAllDataFieldsAndReleaseByteSource();
        super.deallocate();
    }

    @Override
    public void close() {
        for (final AbstractField field : fieldsOrdered) {
            field.close();
        }
    }

    @Nonnull
    public AbstractField[] getFieldsOrdered() {
        return fieldsOrdered;
    }
}
