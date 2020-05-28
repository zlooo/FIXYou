package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.FIXYouException;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.utils.FieldTypeUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import lombok.ToString;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Hashing;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntHashSet;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Slf4j
@ToString(callSuper = true)
public class GroupField extends AbstractField {

    private final FixSpec.FieldNumberTypePair[] childFields;
    private final IntHashSet memberNumbers;
    private Repetition[] repetitions;
    private final Supplier<Repetition> repetitionSupplier;
    private final ByteBuf fieldDataWithoutRepetitionCount;
    private int numberOfRepetitions;
    private int numberOfRepetitionsRead;

    public GroupField(int number, FixSpec spec) {
        super(number, DefaultConfiguration.FIELD_BUFFER_SIZE, true);
        this.fieldDataWithoutRepetitionCount = Unpooled.directBuffer(DefaultConfiguration.FIELD_BUFFER_SIZE);
        this.childFields = spec.getChildPairSpec(number);
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

    public int numberOfFieldsInGroup() {
        return childFields.length;
    }

    public GroupField write() {
        reset();
        return this;
    }

    public GroupField writeNext() {
        fieldDataWithoutRepetitionCount.writeByte(FixMessage.FIELD_SEPARATOR);
        for (final AbstractField childField : repetitions[numberOfRepetitions].fieldsOrdered) {
            fieldDataWithoutRepetitionCount.writeBytes(childField.encodedFieldNumber.readerIndex(0)).writeBytes(childField.fieldData).writeByte(FixMessage.FIELD_SEPARATOR);
        }
        fieldDataWithoutRepetitionCount.writerIndex(fieldDataWithoutRepetitionCount.writerIndex() - 1);
        ensureRepetitionsArrayCapacity(++numberOfRepetitions);
        FieldUtils.writeEncoded(numberOfRepetitions, fieldData.clear());
        fieldData.writeBytes(fieldDataWithoutRepetitionCount.readerIndex(0));
        return this;
    }

    @Override
    protected void resetInnerState() {
        for (final Repetition repetition : repetitions) {
            for (final AbstractField field : repetition.fieldsOrdered) {
                field.reset();
            }
        }
        numberOfRepetitions = 0;
        numberOfRepetitionsRead = 0;
        fieldDataWithoutRepetitionCount.clear();
        fieldData.clear();
    }

    @Nullable
    public <T extends AbstractField> T getField(int repetition, int fieldNum) {
        return (T) repetitions[repetition].idToField.get(fieldNum);
    }

    @Nullable
    public <T extends AbstractField> T getFieldAndIncRepetitionIfValueIsSet(int fieldNum) { //TODO this method is not ok, it does not check repetitions.length so it'll potentially throw ArrayIndexOutOfBoundsException
        final AbstractField field = repetitions[numberOfRepetitions].idToField.get(fieldNum);
        if (field.isValueSet()) {
            if (numberOfRepetitions < numberOfRepetitionsRead - 1) {
                return (T) repetitions[++numberOfRepetitions].idToField.get(fieldNum);
            } else {
                throw new FIXYouException("More repetitions than NumInGroup said it would be? Field " + getNumber() + ", value " + numberOfRepetitionsRead);
            }
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
        final int fieldDataWithoutRepetitionCountRefCount = fieldDataWithoutRepetitionCount.refCnt();
        if (fieldDataWithoutRepetitionCountRefCount > 0) {
            fieldDataWithoutRepetitionCount.release(fieldDataWithoutRepetitionCountRefCount);
        }
        super.close();
    }

    private void ensureRepetitionsArrayCapacity(int capacity) {
        if (repetitions.length < capacity) {
            log.warn("Repetitions array resize in field {}, from {} to {}. Consider making default larger", number, repetitions.length, capacity);
            final Repetition[] newArray = (Repetition[]) Array.newInstance(Repetition.class, capacity);
            System.arraycopy(repetitions, 0, newArray, 0, repetitions.length);
            for (int i = repetitions.length; i < capacity; i++) {
                newArray[i] = repetitionSupplier.get();
            }
            repetitions = newArray;
        }
    }

    public void parseRepetitionsNumber() {
        numberOfRepetitionsRead = ((AsciiString) fieldData.readCharSequence(fieldData.writerIndex(), StandardCharsets.US_ASCII)).parseInt();
        ensureRepetitionsArrayCapacity(numberOfRepetitionsRead);
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
