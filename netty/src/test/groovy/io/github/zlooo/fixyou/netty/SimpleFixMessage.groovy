package io.github.zlooo.fixyou.netty

import com.carrotsearch.hppcrt.*
import com.carrotsearch.hppcrt.maps.*
import com.carrotsearch.hppcrt.sets.IntHashSet
import com.carrotsearch.hppcrt.sets.LongHashSet
import io.github.zlooo.fixyou.DefaultConfiguration
import io.github.zlooo.fixyou.commons.AbstractPoolableFixMessage
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.model.FixSpec
import io.github.zlooo.fixyou.parser.model.FixMessageRepeatingGroupUtils
import io.github.zlooo.fixyou.parser.model.FixMessageToString
import io.netty.buffer.ByteBuf
import io.netty.util.AbstractReferenceCounted
import io.netty.util.ReferenceCounted

import java.nio.charset.StandardCharsets

class SimpleFixMessage extends AbstractPoolableFixMessage implements FixMessage {

    private final IntSet fieldsSet = new IntHashSet(DefaultConfiguration.INITIAL_FIELDS_IN_MSG_NUMBER)
    private final LongSet repeatingGroupFieldsSet = new LongHashSet(DefaultConfiguration.INITIAL_FIELDS_IN_MSG_NUMBER)
    private final IntSet booleanValues = new IntHashSet()
    private final LongSet booleanRepeatingGroupValues = new LongHashSet()
    private final IntCharMap charValues = new IntCharHashMap()
    private final LongCharMap charRepeatingGroupValues = new LongCharHashMap()
    private final IntObjectMap<CharSequence> charSequenceValues = new IntObjectHashMap<>()
    private final LongObjectMap<CharSequence> charSequenceRepeatingGroupValues = new LongObjectHashMap<>()
    private final IntLongMap doubleUnscaledValues = new IntLongHashMap()
    private final LongLongMap doubleUnscaledRepeatingGroupValues = new LongLongHashMap()
    private final IntShortMap scaleValues = new IntShortHashMap()
    private final LongShortMap scaleRepeatingGroupValues = new LongShortHashMap()
    private final IntLongMap longValues = new IntLongHashMap()
    private final LongLongMap longRepeatingGroupValues = new LongLongHashMap()
    private final IntLongMap timestampValues = new IntLongHashMap()
    private final LongLongMap timestampRepeatingGroupValues = new LongLongHashMap()
    private int bodyLength

    SimpleFixMessage() {
        charSequenceValues.setDefaultValue("")
        charSequenceRepeatingGroupValues.setDefaultValue("")
    }

    @Override
    void reset() {
        booleanValues.clear()
        booleanRepeatingGroupValues.clear()
        charValues.clear()
        charRepeatingGroupValues.clear()
        charSequenceValues.clear()
        charSequenceRepeatingGroupValues.clear()
        doubleUnscaledValues.clear()
        doubleUnscaledRepeatingGroupValues.clear()
        scaleValues.clear()
        scaleRepeatingGroupValues.clear()
        longValues.clear()
        longRepeatingGroupValues.clear()
        timestampValues.clear()
        timestampRepeatingGroupValues.clear()
        bodyLength = 0
        fieldsSet.clear()
        repeatingGroupFieldsSet.clear()
    }

    @Override
    boolean getBooleanValue(int fieldNumber) {
        return booleanValues.contains(fieldNumber)
    }

    @Override
    boolean getBooleanValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return booleanRepeatingGroupValues.contains(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber))
    }

    @Override
    void setBooleanValue(int fieldNumber, boolean newValue) {
        if (newValue) {
            booleanValues.add(fieldNumber)
        } else {
            booleanValues.remove(fieldNumber)
        }
        fieldsSet.add(fieldNumber)
    }

    @Override
    void setBooleanValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, boolean newValue) {
        final long repeatingGroupKey = FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)
        if (newValue) {
            booleanRepeatingGroupValues.add(repeatingGroupKey)
        } else {
            booleanRepeatingGroupValues.remove(repeatingGroupKey)
        }
        repeatingGroupFieldsSet.add(repeatingGroupKey)
    }

    @Override
    char getCharValue(int fieldNumber) {
        return charValues.get(fieldNumber)
    }

    @Override
    char getCharValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return charRepeatingGroupValues.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber))
    }

    @Override
    void setCharValue(int fieldNumber, char newValue) {
        charValues.put(fieldNumber, newValue)
        fieldsSet.add(fieldNumber)
    }

    @Override
    void setCharValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char newValue) {
        final long repeatingGroupKey = FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)
        charRepeatingGroupValues.put(repeatingGroupKey, newValue)
        repeatingGroupFieldsSet.add(repeatingGroupKey)
    }

    @Override
    CharSequence getCharSequenceValue(int fieldNumber) {
        return charSequenceValues.get(fieldNumber)
    }

    @Override
    CharSequence getCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return charSequenceRepeatingGroupValues.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber))
    }

    @Override
    char getCharSequenceLength(int fieldNumber) {
        return (char) charSequenceValues.get(fieldNumber).length()
    }

    @Override
    char getCharSequenceLength(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return (char) charSequenceRepeatingGroupValues.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)).length()
    }

    @Override
    void setCharSequenceValue(int fieldNumber, CharSequence newValue) {
        charSequenceValues.put(fieldNumber, newValue)
        fieldsSet.add(fieldNumber)
    }

    @Override
    void setCharSequenceValue(int fieldNumber, ByteBuf asciiByteBuffer) {
        charSequenceValues.put(fieldNumber, asciiByteBuffer.toString(StandardCharsets.US_ASCII))
        fieldsSet.add(fieldNumber)
    }

    @Override
    void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, CharSequence newValue) {
        final long repeatingGroupKey = FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)
        charSequenceRepeatingGroupValues.put(repeatingGroupKey, newValue)
        repeatingGroupFieldsSet.add(repeatingGroupKey)
    }

    @Override
    void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, ByteBuf asciiByteBuffer) {
        final long repeatingGroupKey = FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)
        charSequenceRepeatingGroupValues.put(repeatingGroupKey, asciiByteBuffer.toString(StandardCharsets.US_ASCII))
        repeatingGroupFieldsSet.add(repeatingGroupKey)
    }

    @Override
    void setCharSequenceValue(int fieldNumber, char[] newValue) {
        charSequenceValues.put(fieldNumber, String.copyValueOf(newValue))
        fieldsSet.add(fieldNumber)
    }

    @Override
    void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char[] newValue) {
        final long repeatingGroupKey = FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)
        charSequenceRepeatingGroupValues.put(repeatingGroupKey, String.copyValueOf(newValue))
        repeatingGroupFieldsSet.add(repeatingGroupKey)
    }

    @Override
    void setCharSequenceValue(int fieldNumber, char[] newValue, int newValueLength) {
        charSequenceValues.put(fieldNumber, String.copyValueOf(newValue, 0, newValueLength))
        fieldsSet.add(fieldNumber)
    }

    @Override
    void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char[] newValue, int newValueLength) {
        final long repeatingGroupKey = FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)
        charSequenceRepeatingGroupValues.put(repeatingGroupKey, String.copyValueOf(newValue, 0, newValueLength))
        repeatingGroupFieldsSet.add(repeatingGroupKey)
    }

    @Override
    long getDoubleUnscaledValue(int fieldNumber) {
        return doubleUnscaledValues.get(fieldNumber)
    }

    @Override
    long getDoubleUnscaledValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return doubleUnscaledRepeatingGroupValues.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber))
    }

    @Override
    short getScale(int fieldNumber) {
        return scaleValues.get(fieldNumber)
    }

    @Override
    short getScale(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return scaleRepeatingGroupValues.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber))
    }

    @Override
    void setDoubleValue(int fieldNumber, long newValue, short newScale) {
        doubleUnscaledValues.put(fieldNumber, newValue)
        scaleValues.put(fieldNumber, newScale)
        fieldsSet.add(fieldNumber)
    }

    @Override
    void setDoubleValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue, short newScale) {
        final long repeatingGroupKey = FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)
        doubleUnscaledRepeatingGroupValues.put(repeatingGroupKey, newValue)
        scaleRepeatingGroupValues.put(repeatingGroupKey, newScale)
        repeatingGroupFieldsSet.add(repeatingGroupKey)
    }

    @Override
    long getLongValue(int fieldNumber) {
        return longValues.get(fieldNumber)
    }

    @Override
    long getLongValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return longRepeatingGroupValues.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber))
    }

    @Override
    void setLongValue(int fieldNumber, long newValue) {
        longValues.put(fieldNumber, newValue)
        fieldsSet.add(fieldNumber)
    }

    @Override
    void setLongValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue) {
        final long repeatingGroupKey = FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)
        longRepeatingGroupValues.put(repeatingGroupKey, newValue)
        repeatingGroupFieldsSet.add(repeatingGroupKey)
    }

    @Override
    long getTimestampValue(int fieldNumber) {
        return timestampValues.get(fieldNumber)
    }

    @Override
    long getTimestampValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return timestampRepeatingGroupValues.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber))
    }

    @Override
    void setTimestampValue(int fieldNumber, long newValue) {
        timestampValues.put(fieldNumber, newValue)
        fieldsSet.add(fieldNumber)
    }

    @Override
    void setTimestampValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue) {
        final long repeatingGroupKey = FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)
        timestampRepeatingGroupValues.put(repeatingGroupKey, newValue)
        repeatingGroupFieldsSet.add(repeatingGroupKey)
    }

    @Override
    boolean isValueSet(int fieldNumber) {
        return fieldsSet.contains(fieldNumber)
    }

    @Override
    boolean isValueSet(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return repeatingGroupFieldsSet.contains(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber))
    }

    @Override
    void removeField(int fieldNumber) {
        booleanValues.remove(fieldNumber)
        charValues.remove(fieldNumber)
        charSequenceValues.remove(fieldNumber)
        doubleUnscaledValues.remove(fieldNumber)
        scaleValues.remove(fieldNumber)
        longValues.remove(fieldNumber)
        timestampValues.remove(fieldNumber)
        fieldsSet.remove(fieldNumber)
    }

    @Override
    IntCollection setFields() {
        return fieldsSet
    }

    @Override
    int getBodyLength() {
        return bodyLength
    }

    @Override
    void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength
    }

    @Override
    String toString(boolean wholeMessage, FixSpec fixSpec) {
        return FixMessageToString.toString(this, wholeMessage, fixSpec)
    }

    @Override
    protected void deallocate() {

    }

    @Override
    ReferenceCounted touch(Object hint) {
        return this
    }

    @Override
    void close() {
        //nothing to do
    }

    @Override
    void copyDataFrom(Object source) {
        throw new UnsupportedOperationException()
    }
}
