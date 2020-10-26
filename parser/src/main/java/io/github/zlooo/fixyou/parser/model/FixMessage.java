package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.commons.pool.AbstractPoolableObject;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

@Getter
public class FixMessage extends AbstractPoolableObject {

    public static final byte FIELD_SEPARATOR = AsciiCodes.SOH;
    public static final byte FIELD_VALUE_SEPARATOR = AsciiCodes.EQUALS;
    private static final int NOT_SET = -1;
    private static final Field PLACEHOLDER = new PlaceholderField();
    private static final double LOAD_FACTOR = 1.2;

    private final FieldCodec fieldCodec;
    private Field[] allFields;
    private Field[] actualFields;
    private int actualFieldsLength;
    private ByteBufComposer messageByteSource;
    @Setter
    private int startIndex = NOT_SET;
    @Setter
    private int endIndex = NOT_SET;

    public FixMessage(@Nonnull FieldCodec fieldCodec) {
        this.fieldCodec = fieldCodec;
        allFields = new Field[DefaultConfiguration.DEFAULT_MAX_FIELD_NUMBER + 1];
        actualFields = new Field[DefaultConfiguration.DEFAULT_MAX_FIELD_NUMBER];
        Arrays.fill(allFields, PLACEHOLDER);
    }

    public void setMessageByteSource(ByteBufComposer newMessageByteSource) {
        this.messageByteSource = newMessageByteSource;
        for (int i = 0; i < actualFieldsLength; i++) {
            ArrayUtils.getElementAt(actualFields, i).setFieldData(newMessageByteSource);
        }
    }

    @Nullable
    public Field getField(int number) {
        ensureArraysLengthIsSufficient(number);
        Field field = allFields[number];
        if (field == PLACEHOLDER) {
            field = new Field(number, fieldCodec);
            allFields[number] = field;
            actualFields[actualFieldsLength++] = field;
            field.setFieldData(messageByteSource);
        }
        return field;
    }

    public Field getFieldOrPlaceholder(int index) {
        if (allFields.length - 1 < index) {
            return PLACEHOLDER;
        } else {
            return ArrayUtils.getElementAt(allFields, index);
        }
    }

    private void ensureArraysLengthIsSufficient(int index) {
        if (allFields.length - 1 < index) {
            final Field[] newAllFieldsArray = new Field[(int) (Math.max(allFields.length * LOAD_FACTOR, index + 1))];
            final Field[] newActualFieldsArray = new Field[newAllFieldsArray.length];
            System.arraycopy(allFields, 0, newAllFieldsArray, 0, allFields.length);
            for (int i = allFields.length; i < newAllFieldsArray.length; i++) {
                newAllFieldsArray[i] = PLACEHOLDER;
            }
            System.arraycopy(actualFields, 0, newActualFieldsArray, 0, actualFieldsLength);
            allFields = newAllFieldsArray;
            actualFields = newActualFieldsArray;
        }
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean wholeMessage) {
        return FixMessageToString.toString(this, wholeMessage);
    }

    public void resetAllDataFieldsAndReleaseByteSource() {
        for (int i = 0; i < actualFieldsLength; i++) {
            final Field field = ArrayUtils.getElementAt(actualFields, i);
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
        for (int i = 0; i < actualFieldsLength; i++) {
            ArrayUtils.getElementAt(actualFields, i).close();
        }
    }

    private static final class PlaceholderField extends Field {

        public PlaceholderField() {
            super(NOT_SET, null);
        }

        @Override
        public boolean isValueSet() {
            return false;
        }

        @Override
        protected FieldValue createFieldValue() {
            return null; //we do nto want to create any value for this field
        }

        @Override
        public void close() {
            //nothing to do we do not want to close placeholder field, EVER
        }
    }
}