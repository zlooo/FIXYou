package io.github.zlooo.fixyou.model;

import com.carrotsearch.hppcrt.IntCollection;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;

public interface FixMessage extends Resettable {
    byte FIELD_SEPARATOR = AsciiCodes.SOH;
    byte FIELD_VALUE_SEPARATOR = AsciiCodes.EQUAL;

    boolean getBooleanValue(int fieldNumber);

    boolean getBooleanValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex);

    void setBooleanValue(int fieldNumber, boolean newValue);

    void setBooleanValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, boolean newValue);

    char getCharValue(int fieldNumber);

    char getCharValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex);

    void setCharValue(int fieldNumber, char newValue);

    void setCharValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char newValue);

    CharSequence getCharSequenceValue(int fieldNumber);

    CharSequence getCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex);

    char getCharSequenceLength(int fieldNumber);

    char getCharSequenceLength(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex);

    void setCharSequenceValue(int fieldNumber, ByteBuf asciiByteBuffer);

    void setCharSequenceValue(int fieldNumber, CharSequence newValue);

    void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, CharSequence newValue);

    void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, ByteBuf asciiByteBuffer);

    void setCharSequenceValue(int fieldNumber, char[] newValue);

    void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char[] newValue);

    void setCharSequenceValue(int fieldNumber, char[] newValue, int newValueLength);

    void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char[] newValue, int newValueLength);

    long getDoubleUnscaledValue(int fieldNumber);

    long getDoubleUnscaledValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex);

    short getScale(int fieldNumber);

    short getScale(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex);

    void setDoubleValue(int fieldNumber, long newValue, short newScale);

    void setDoubleValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue, short newScale);

    long getLongValue(int fieldNumber);

    long getLongValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex);

    void setLongValue(int fieldNumber, long newValue);

    void setLongValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue);

    long getTimestampValue(int fieldNumber);

    long getTimestampValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex);

    void setTimestampValue(int fieldNumber, long newValue);

    void setTimestampValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue);

    boolean isValueSet(int fieldNumber);

    boolean isValueSet(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex);

    void removeField(int fieldNumber);

    IntCollection setFields();

    int getBodyLength();

    void setBodyLength(int bodyLength);

    String toString(boolean wholeMessage, FixSpec fixSpec);
}
