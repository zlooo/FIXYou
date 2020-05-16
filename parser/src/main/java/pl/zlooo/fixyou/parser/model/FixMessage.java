package pl.zlooo.fixyou.parser.model;

import lombok.Getter;
import pl.zlooo.fixyou.commons.pool.AbstractPoolableObject;
import pl.zlooo.fixyou.model.FieldType;
import pl.zlooo.fixyou.model.FixSpec;
import pl.zlooo.fixyou.parser.utils.FieldTypeUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

@Getter
public class FixMessage extends AbstractPoolableObject {

    public static final byte FIELD_SEPARATOR = 0x01;
    private static final char FIELD_DELIMITER = '|';
    private static final char EQUALS_CHAR = '=';
    private static final String SOH = "\u0001";
    private static final int LONG_MESSAGE_FIELD_NUMBER_THRESHOLD = 10;

    private final AbstractField[] fieldsOrdered;
    private final AbstractField[] fields;

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
                builder.append(field.number).append(EQUALS_CHAR).append(field.fieldData.toString(StandardCharsets.US_ASCII)).append(FIELD_DELIMITER);
            }
        } else {
            for (final AbstractField field : fieldsOrdered) {
                builder.append(field.number).append(EQUALS_CHAR).append(field.fieldData.toString(StandardCharsets.US_ASCII)).append(FIELD_DELIMITER);
            }
        }
        builder.deleteCharAt(builder.length() - 1).append(longMessage ? "..." : "").append(", refCnt=").append(refCnt());
        return builder.toString();
    }

    public void resetAllDataFields() {
        for (final AbstractField field : fields) { //TODO run JMH and see if there is a difference between fields and fieldsOrdered. Probably not but make sure
            if (field != null) { //since index in fields table is equal to field number, it's quite possible not all positions in this table will be filled with actual objects
                field.reset();
            }
        }
    }

    public void resetDataFields(int... excludes) {
        fieldLoop:
        for (final AbstractField field : fields) {
            if (field == null) { //since index in fields table is equal to field number, it's quite possible not all positions in this table will be filled with actual objects
                continue;
            }
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
