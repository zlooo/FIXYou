package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Hashing;
import org.agrona.collections.Int2ObjectHashMap;

import javax.annotation.Nullable;

@Data
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@Slf4j
@ToString
class FieldValue implements Resettable, Closeable {
    static final char CHAR_DEFAULT_VALUE = Character.MIN_VALUE;
    static final long LONG_DEFAULT_VALUE = Long.MIN_VALUE;
    static final int FIELD_DATA_STARTING_LENGTH = 21; //driven by timestamp field - yyyyMMdd-HH:mm:ss.SSS

    private final MutableCharSequence charSequenceValue = new MutableCharSequence();
    @Setter(AccessLevel.NONE)
    private Repetition[] repetitions;
    private int repetitionCounter;
    private boolean parsed;
    private boolean booleanValue;
    private char charValue = CHAR_DEFAULT_VALUE;
    @Setter(AccessLevel.NONE)
    private byte charValueRaw = CHAR_DEFAULT_VALUE;
    private int length;
    private ByteBuf rawValue = Unpooled.directBuffer(FIELD_DATA_STARTING_LENGTH);
    private char[] charArrayValue = new char[FIELD_DATA_STARTING_LENGTH];
    private long longValue = LONG_DEFAULT_VALUE;
    private short scale;
    private int sumOfBytes;
    private FieldType valueTypeSet;

    FieldValue() {
        charSequenceValue.setState(charArrayValue);
        this.repetitions = new Repetition[DefaultConfiguration.NUMBER_OF_REPETITIONS_IN_GROUP];
        for (int i = 0; i < repetitions.length; i++) {
            repetitions[i] = new Repetition();

        }
    }

    void setCharValue(char charValue) {
        this.charValue = charValue;
        this.charValueRaw = AsciiString.c2b(this.charValue);
    }

    void setLength(int length) {
        this.length = length;
        this.charSequenceValue.setLength(length);
    }

    @Override
    public void reset() {
        parsed = false;
        charValue = CHAR_DEFAULT_VALUE;
        length = 0;
        charSequenceValue.setLength(length);
        longValue = LONG_DEFAULT_VALUE;
        scale = 0;
        sumOfBytes = 0;
        valueTypeSet = null;
        repetitionCounter = 0;
        for (final Repetition repetition : repetitions) {
            for (final Field field : repetition.idToField.values()) {
                field.reset();
            }
        }
    }

    @Override
    public void close() {
        rawValue.release();
        for (final Repetition repetition : repetitions) {
            for (final Field field : repetition.idToField.values()) {
                field.close();
            }
        }
    }

    Repetition getCurrentRepetition() {
        ensureRepetitionsArrayCapacity();
        return repetitions[repetitionCounter];
    }

    private void ensureRepetitionsArrayCapacity() {
        if (repetitions.length <= repetitionCounter) {
            final int newLength = (int) ((repetitionCounter + 1) * (1.0 + Hashing.DEFAULT_LOAD_FACTOR));
            log.warn("Repetitions array resize from {} to {}. Consider making default larger", repetitions.length, newLength);
            final Repetition[] newArray = new Repetition[newLength];
            System.arraycopy(repetitions, 0, newArray, 0, repetitions.length);
            for (int i = repetitions.length; i < newLength; i++) {
                newArray[i] = new Repetition();
            }
            repetitions = newArray;
        }
    }

    void ensureCharArraysCapacity(int newLength) {
        if (charArrayValue.length < newLength) {
            final int newTableLength = (int) ((newLength + 1) * (1.0 + Hashing.DEFAULT_LOAD_FACTOR));
            rawValue.ensureWritable(newTableLength);
            final char[] newCharArrayValue = new char[newTableLength];
            charArrayValue = newCharArrayValue;
            charSequenceValue.setState(newCharArrayValue);
        }
    }

    void incrementRepetitionsCounter() {
        repetitionCounter = repetitionCounter + 1;
        ensureRepetitionsArrayCapacity();
    }

    void copyDataFrom(FieldValue fieldValue, FieldCodec fieldCodec) {
        repetitionCounter = fieldValue.repetitionCounter;
        final int repetitionsArrayLength = fieldValue.repetitions.length;
        if (repetitionsArrayLength > 0) {
            ensureRepetitionsArrayCapacity();
            for (int i = 0; i < repetitionsArrayLength; i++) {
                final Repetition currentRepetition = ArrayUtils.getElementAt(repetitions, i);
                for (final Field groupField : ArrayUtils.getElementAt(fieldValue.repetitions, i).idToField.values()) {
                    currentRepetition.getExistingOrNewGroupField(groupField.getNumber(), fieldCodec).copyDataFrom(groupField);
                }

            }
        }
        parsed = fieldValue.parsed;
        booleanValue = fieldValue.booleanValue;
        charValue = fieldValue.charValue;
        charValueRaw = fieldValue.charValueRaw;
        this.length = fieldValue.length;
        charSequenceValue.setLength(this.length);
        rawValue.clear().writeBytes(fieldValue.rawValue, 0, this.length);
        final char[] sourceCharArray = fieldValue.charArrayValue;
        final int sourceCharArrayLength = sourceCharArray.length;
        ensureCharArraysCapacity(sourceCharArrayLength);
        System.arraycopy(sourceCharArray, 0, this.charArrayValue, 0, sourceCharArrayLength);
        longValue = fieldValue.longValue;
        scale = fieldValue.scale;
        sumOfBytes = fieldValue.sumOfBytes;
        valueTypeSet = fieldValue.valueTypeSet;
    }

    static final class Repetition {
        private final Int2ObjectHashMap<Field> idToField = new Int2ObjectHashMap<>();

        public Field getExistingOrNewGroupField(int fieldNum, FieldCodec fieldCodec) {
            return idToField.computeIfAbsent(fieldNum, key -> new Field(key, fieldCodec));
        }

        @Nullable
        public Field getExistingGroupFieldOrNull(int fieldNumber) {
            final Field field = idToField.get(fieldNumber);
            if (field != null) {
                return field;
            } else {
                return null;
            }
        }
    }
}
