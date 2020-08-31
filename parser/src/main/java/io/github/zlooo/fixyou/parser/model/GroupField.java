package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.utils.FieldTypeUtils;
import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Hashing;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntHashSet;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class GroupField extends AbstractField {

    public static final long DEFAULT_VALUE = 0;
    public static final int FIELD_DATA_LENGTH = 7;
    private final Supplier<Repetition> repetitionSupplier;
    private final IntHashSet memberNumbers;
    private Repetition[] repetitions;
    private int repetitionCounter;

    private long value = DEFAULT_VALUE;
    private byte[] rawValue = new byte[FIELD_DATA_LENGTH];
    private char[] unparsedValue = new char[FIELD_DATA_LENGTH];

    public GroupField(int number, FixSpec spec) {
        super(number);
        final FixSpec.FieldNumberTypePair[] childFields = spec.getChildPairSpec(number);
        if (childFields == null || childFields.length == 0) {
            throw new IllegalArgumentException("Group must consist of at least 1 child field, group number " + number);
        }
        this.memberNumbers = new IntHashSet(childFields.length);
        for (final FixSpec.FieldNumberTypePair childField : childFields) {
            memberNumbers.add(childField.getFieldNumber());
        }
        repetitionSupplier = () -> {
            final Int2ObjectHashMap<AbstractField> idToFieldMap = new Int2ObjectHashMap<>(childFields.length, Hashing.DEFAULT_LOAD_FACTOR);
            final AbstractField[] fieldsOrdered = new AbstractField[childFields.length];
            for (int i = 0; i < childFields.length; i++) {
                final FixSpec.FieldNumberTypePair childField = childFields[i];
                final int fieldNumber = childField.getFieldNumber();
                final AbstractField field = FieldTypeUtils.createField(childField.getFieldType(), fieldNumber, spec);
                idToFieldMap.put(fieldNumber, field);
                fieldsOrdered[i] = field;
            }
            return new Repetition(fieldsOrdered, idToFieldMap);
        };
        this.repetitions = new Repetition[DefaultConfiguration.NUMBER_OF_REPETITIONS_IN_GROUP];
        for (int i = 0; i < repetitions.length; i++) {
            repetitions[i] = repetitionSupplier.get();

        }
    }

    @Override
    protected void setFieldData(ByteBufComposer fieldData) {
        super.setFieldData(fieldData);
        for (final Repetition repetition : repetitions) {
            for (final AbstractField repeatingGroupField : repetition.fieldsOrdered) {
                repeatingGroupField.setFieldData(fieldData);
            }

        }

    }

    public long getValue() {
        if (value == DEFAULT_VALUE && valueSet) {
            value = ParsingUtils.parseLong(fieldData, startIndex, FixMessage.FIELD_SEPARATOR); //TODO run JMH test and see if you're right
        }
        return value;
    }

    public void setValue(long value) {
        this.value = value;
        this.valueSet = true;
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.GROUP;
    }

    @Override
    protected void resetInnerState() {
        value = DEFAULT_VALUE;
        repetitionCounter = 0;
        for (final Repetition repetition : repetitions) {
            for (final AbstractField field : repetition.fieldsOrdered) {
                field.reset();
            }
        }
    }

    @Nullable
    public <T extends AbstractField> T getField(int repetition, int fieldNum) {
        return (T) repetitions[repetition].idToField.get(fieldNum);
    }

    @Nullable
    public <T extends AbstractField> T getFieldForCurrentRepetition(int fieldNum) {
        ensureRepetitionsArrayCapacity();
        return (T) repetitions[repetitionCounter].idToField.get(fieldNum);
    }

    public GroupField next() {
        repetitionCounter++;
        ensureRepetitionsArrayCapacity();
        return this;
    }

    @Override
    public boolean isValueSet() {
        if (repetitionCounter == 0) {
            boolean result = false;
            if (repetitions.length > 0) {
                for (final AbstractField field : repetitions[0].fieldsOrdered) {
                    result |= field.isValueSet();
                }
            }
            return result;
        } else {
            return true;
        }
    }

    @Override
    public void appendByteBufWithValue(ByteBuf out) {
        FieldUtils.writeEncoded(repetitionCounter, out);
        out.writeByte(FixMessage.FIELD_SEPARATOR);
        for (int i = 0; i <= repetitionCounter; i++) {
            for (final AbstractField repetitionField : repetitions[i].fieldsOrdered) {
                if (repetitionField.isValueSet()) {
                    repetitionField.appendFieldNumber(out);
                    repetitionField.appendByteBufWithValue(out);
                    out.writeByte(FixMessage.FIELD_SEPARATOR);
                }
            }
        }
        out.writerIndex(out.writerIndex() - 1); //"remove" last soh from buffer
    }

    @Override
    public void close() {
        for (final Repetition repetition : repetitions) {
            for (final AbstractField childField : repetition.fieldsOrdered) {
                childField.close();
            }
        }
        super.close();
    }

    private void ensureRepetitionsArrayCapacity() {
        if (repetitions.length <= repetitionCounter) {
            final int newLength = (int) ((repetitionCounter + 1) * (1.0 + Hashing.DEFAULT_LOAD_FACTOR));
            log.warn("Repetitions array resize in field {}, from {} to {}. Consider making default larger", number, repetitions.length, newLength);
            final Repetition[] newArray = new Repetition[newLength];
            System.arraycopy(repetitions, 0, newArray, 0, repetitions.length);
            for (int i = repetitions.length; i < newLength; i++) {
                final Repetition repetition = repetitionSupplier.get();
                if (fieldData != null) {
                    for (final AbstractField field : repetition.fieldsOrdered) {
                        field.setFieldData(fieldData);
                    }

                }
                newArray[i] = repetition;
            }
            repetitions = newArray;
        }
    }

    public boolean containsField(int fieldNum) {
        return memberNumbers.contains(fieldNum);
    }

    @Value
    private static final class Repetition {
        private final AbstractField[] fieldsOrdered;
        private final Int2ObjectHashMap<? extends AbstractField> idToField;
    }
}
