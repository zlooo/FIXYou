package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.commons.pool.AbstractPoolableObject;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.FakeFixSpec;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
public class FixMessage extends AbstractPoolableObject {

    public static final byte FIELD_SEPARATOR = AsciiCodes.SOH;
    public static final byte FIELD_VALUE_SEPARATOR = AsciiCodes.EQUALS;
    private static final int NOT_SET = -1;
    private static final Field PLACEHOLDER = new Field(NOT_SET, FakeFixSpec.INSTANCE, null) {
        @Override
        public void close() {
            //nothing to do we do not want to close placeholder field, EVER
        }
    };

    private final FixSpec fixSpec;
    private final FieldCodec fieldCodec;
    private final Field[] fieldsOrdered;
    private final Field[] fields;
    private ByteBufComposer messageByteSource;
    @Setter
    private int startIndex = NOT_SET;
    @Setter
    private int endIndex = NOT_SET;

    public FixMessage(@Nonnull FixSpec spec, @Nonnull FieldCodec fieldCodec) {
        this.fixSpec = spec;
        this.fieldCodec = fieldCodec;
        final int[] fieldsOrder = spec.getFieldsOrder();
        fieldsOrdered = new Field[fieldsOrder.length];
        fields = new Field[spec.highestFieldNumber() + 1];
        for (int i = 0; i < fieldsOrder.length; i++) {
            final int fieldNumber = fieldsOrder[i];
            fieldsOrdered[i] = PLACEHOLDER;
            fields[fieldNumber] = PLACEHOLDER;
        }
    }

    public void setMessageByteSource(ByteBufComposer newMessageByteSource) {
        this.messageByteSource = newMessageByteSource;
        for (final Field field : fieldsOrdered) {
            field.setFieldData(newMessageByteSource);
        }
    }

    @Nullable
    public Field getField(int number) {
        Field field = fields[number];
        if (field == PLACEHOLDER) {
            field = new Field(number, fixSpec, fieldCodec);
            fields[number] = field;
            final int fieldIndex = ArrayUtils.indexOf(fixSpec.getFieldsOrder(), number);
            fieldsOrdered[fieldIndex] = field;
            field.setFieldData(messageByteSource);
        }
        return field;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean wholeMessage) {
        return FixMessageToString.toString(this, wholeMessage);
    }

    public void resetAllDataFieldsAndReleaseByteSource() {
        for (final Field field : fieldsOrdered) {
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
        for (final Field field : fieldsOrdered) {
            field.close();
        }
    }

    @Nonnull
    public Field[] getFieldsOrdered() {
        return fieldsOrdered;
    }
}
