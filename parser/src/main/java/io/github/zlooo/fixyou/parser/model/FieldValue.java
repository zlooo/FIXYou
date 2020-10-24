package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Hashing;
import org.agrona.collections.Int2ObjectHashMap;

import java.util.function.Supplier;

@Data
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@Slf4j
@ToString(exclude = {"repetitionSupplier"})
class FieldValue implements Resettable, Closeable {
    static final char CHAR_DEFAULT_VALUE = Character.MIN_VALUE;
    static final long LONG_DEFAULT_VALUE = Long.MIN_VALUE;
    static final int FIELD_DATA_STARTING_LENGTH = 21; //driven by timestamp field - yyyyMMdd-HH:mm:ss.SSS

    private final MutableCharSequence charSequenceValue = new MutableCharSequence();
    private final Supplier<Repetition> repetitionSupplier;
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

    FieldValue(Supplier<Repetition> repetitionSupplier) {
        charSequenceValue.setState(charArrayValue);
        this.repetitionSupplier = repetitionSupplier;
        this.repetitions = new Repetition[DefaultConfiguration.NUMBER_OF_REPETITIONS_IN_GROUP];
        for (int i = 0; i < repetitions.length; i++) {
            repetitions[i] = repetitionSupplier.get();

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
            for (final Field field : repetition.getFieldsOrdered()) {
                field.reset();
            }
        }
    }

    @Override
    public void close() {
        rawValue.release();
        for (final Repetition repetition : repetitions) {
            for (final Field field : repetition.getFieldsOrdered()) {
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
            log.warn("Repetitions array resize in field {}, from {} to {}. Consider making default larger", longValue != LONG_DEFAULT_VALUE ? longValue : "N/A", repetitions.length, newLength);
            final Repetition[] newArray = new Repetition[newLength];
            System.arraycopy(repetitions, 0, newArray, 0, repetitions.length);
            final ByteBufComposer fieldData;
            if (repetitions.length > 0) {
                fieldData = repetitions[0].fieldsOrdered[0].getFieldData();
            } else {
                fieldData = null;
            }
            for (int i = repetitions.length; i < newLength; i++) {
                final Repetition repetition = repetitionSupplier.get();
                if (fieldData != null) {
                    for (final Field field : repetition.fieldsOrdered) {
                        field.setFieldData(fieldData);
                    }

                }
                newArray[i] = repetition;
            }
            repetitions = newArray;
        }
    }

    void incrementRepetitionsCounter() {
        repetitionCounter = repetitionCounter + 1;
        ensureRepetitionsArrayCapacity();
    }

    @Value
    static final class Repetition {
        private final Field[] fieldsOrdered;
        private final Int2ObjectHashMap<? extends Field> idToField;
    }
}
