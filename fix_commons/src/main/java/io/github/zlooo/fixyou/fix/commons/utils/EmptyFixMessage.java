package io.github.zlooo.fixyou.fix.commons.utils;

import com.carrotsearch.hppcrt.IntCollection;
import com.carrotsearch.hppcrt.sets.IntHashSet;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.model.FixSpec;
import io.netty.buffer.ByteBuf;

/**
 * Empty, fake implementation of fix message. Can be used as a placeholder
 */
public class EmptyFixMessage implements FixMessage {

    public static final EmptyFixMessage INSTANCE = new EmptyFixMessage();
    private static final IntCollection EMPTY = new IntHashSet(0);

    @Override
    public boolean getBooleanValue(int fieldNumber) {
        return false;
    }

    @Override
    public boolean getBooleanValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return false;
    }

    @Override
    public void setBooleanValue(int fieldNumber, boolean newValue) {

    }

    @Override
    public void setBooleanValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, boolean newValue) {

    }

    @Override
    public char getCharValue(int fieldNumber) {
        return 0;
    }

    @Override
    public char getCharValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return 0;
    }

    @Override
    public void setCharValue(int fieldNumber, char newValue) {

    }

    @Override
    public void setCharValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char newValue) {

    }

    @Override
    public CharSequence getCharSequenceValue(int fieldNumber) {
        return "";
    }

    @Override
    public CharSequence getCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return "";
    }

    @Override
    public char getCharSequenceLength(int fieldNumber) {
        return 0;
    }

    @Override
    public char getCharSequenceLength(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return 0;
    }

    @Override
    public void setCharSequenceValue(int fieldNumber, CharSequence newValue) {

    }

    @Override
    public void setCharSequenceValue(int fieldNumber, ByteBuf asciiByteBuffer) {

    }

    @Override
    public void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, CharSequence newValue) {

    }

    @Override
    public void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, ByteBuf asciiByteBuffer) {

    }

    @Override
    public void setCharSequenceValue(int fieldNumber, char[] newValue) {

    }

    @Override
    public void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char[] newValue) {

    }

    @Override
    public void setCharSequenceValue(int fieldNumber, char[] newValue, int newValueLength) {

    }

    @Override
    public void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char[] newValue, int newValueLength) {

    }

    @Override
    public long getDoubleUnscaledValue(int fieldNumber) {
        return 0;
    }

    @Override
    public long getDoubleUnscaledValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return 0;
    }

    @Override
    public short getScale(int fieldNumber) {
        return 0;
    }

    @Override
    public short getScale(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return 0;
    }

    @Override
    public void setDoubleValue(int fieldNumber, long newValue, short newScale) {

    }

    @Override
    public void setDoubleValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue, short newScale) {

    }

    @Override
    public long getLongValue(int fieldNumber) {
        return 0;
    }

    @Override
    public long getLongValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return 0;
    }

    @Override
    public void setLongValue(int fieldNumber, long newValue) {

    }

    @Override
    public void setLongValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue) {

    }

    @Override
    public long getTimestampValue(int fieldNumber) {
        return 0;
    }

    @Override
    public long getTimestampValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return 0;
    }

    @Override
    public void setTimestampValue(int fieldNumber, long newValue) {

    }

    @Override
    public void setTimestampValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue) {

    }

    @Override
    public boolean isValueSet(int fieldNumber) {
        return false;
    }

    @Override
    public boolean isValueSet(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return false;
    }

    @Override
    public void removeField(int fieldNumber) {

    }

    @Override
    public IntCollection setFields() {
        return EMPTY;
    }

    @Override
    public int getBodyLength() {
        return 0;
    }

    @Override
    public void setBodyLength(int bodyLength) {

    }

    @Override
    public void reset() {

    }

    @Override
    public String toString(boolean wholeMessage, FixSpec fixSpec) {
        return "EmptyFixMessage[]";
    }
}
