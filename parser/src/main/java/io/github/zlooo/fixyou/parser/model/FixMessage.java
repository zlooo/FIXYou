package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.commons.pool.AbstractPoolableObject;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.utils.FieldTypeUtils;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
public class FixMessage extends AbstractPoolableObject {

    public static final byte FIELD_SEPARATOR = AsciiCodes.SOH;
    public static final byte FIELD_VALUE_SEPARATOR = AsciiCodes.EQUALS;
    private static final int NOT_SET = -1;
    private static final PlaceholderField PLACEHOLDER = new PlaceholderField(NOT_SET);

    private final FixSpec fixSpec;
    private final AbstractField[] fieldsOrdered;
    private final AbstractField[] fields;
    private ByteBufComposer messageByteSource;
    @Setter
    private int startIndex = NOT_SET;
    @Setter
    private int endIndex = NOT_SET;

    public FixMessage(@Nonnull FixSpec spec) {
        fixSpec = spec;
        final int[] fieldsOrder = spec.getFieldsOrder();
        fieldsOrdered = new AbstractField[fieldsOrder.length];
        fields = new AbstractField[spec.highestFieldNumber() + 1];
        for (int i = 0; i < fieldsOrder.length; i++) {
            final int fieldNumber = fieldsOrder[i];
            fieldsOrdered[i] = PLACEHOLDER;
            fields[fieldNumber] = PLACEHOLDER;
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
        T field = (T) fields[number];
        if (field == PLACEHOLDER) {
            final int fieldIndex = ArrayUtils.indexOf(fixSpec.getFieldsOrder(), number);
            field = FieldTypeUtils.createField(fixSpec.getTypes()[fieldIndex], number, fixSpec);
            fields[number] = field;
            fieldsOrdered[fieldIndex] = field;
            field.setFieldData(messageByteSource);
        }
        return field;
    }

    /**
     * Call only for debug purposes, NOT on production. Why? Take a look at how it's implemented ;)
     */
    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean wholeMessage) {
        return FixMessageToString.toString(this, wholeMessage);
    }

    public void resetAllDataFieldsAndReleaseByteSource() {
        for (final AbstractField field : fieldsOrdered) {
            if (field.isValueSet()) {
                field.reset();
            }
        }
        if (messageByteSource != null && holdsData()) {
            messageByteSource.releaseData(startIndex, endIndex);
        }
        startIndex = NOT_SET;
        endIndex = NOT_SET;
    }

    private boolean holdsData() {
        return startIndex != NOT_SET && endIndex != NOT_SET;
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

    private static final class PlaceholderField extends AbstractField {

        private PlaceholderField(int number) {
            super(number);
        }

        @Override
        public FieldType getFieldType() {
            return null;
        }

        @Override
        protected void resetInnerState() { //nothing to do
        }

        @Override
        public int appendByteBufWithValue(ByteBuf out) { //nothing to do
            return 0;
        }

        @Override
        public boolean isValueSet() {
            return false;
        }

        @Override
        public int getLength() {
            return 0;
        }
    }
}
