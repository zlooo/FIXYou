package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.utils.FieldTypeUtils;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Hashing;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntHashSet;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@Slf4j
@ToString(callSuper = true)
public class GroupField extends LongField {

    private final Supplier<Repetition> repetitionSupplier;
    private final IntHashSet memberNumbers;
    private Repetition[] repetitions;
    private int repetitionCounter;

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
    public FieldType getFieldType() {
        return FieldType.GROUP;
    }

    @Override
    protected void resetInnerState() {
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
    public <T extends AbstractField> T getFieldAndIncRepetitionIfValueIsSet(int fieldNum) {
        final AbstractField field = repetitions[repetitionCounter].idToField.get(fieldNum);
        if (field.isValueSet()) {
            repetitionCounter++;
            ensureRepetitionsArrayCapacity();
            return (T) repetitions[repetitionCounter].idToField.get(fieldNum);
        } else {
            return (T) field;
        }
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
        if (repetitions.length < repetitionCounter) {
            final int newLength = (int) (repetitionCounter * (1.0 + Hashing.DEFAULT_LOAD_FACTOR));
            log.warn("Repetitions array resize in field {}, from {} to {}. Consider making default larger", number, repetitions.length, newLength);
            final Repetition[] newArray = new Repetition[newLength];
            System.arraycopy(repetitions, 0, newArray, 0, repetitions.length);
            for (int i = repetitions.length; i < newLength; i++) {
                newArray[i] = repetitionSupplier.get();
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
