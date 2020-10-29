package io.github.zlooo.fixyou.parser.model;

public interface CharSequenceField {
    CharSequence getCharSequenceValue();

    char[] getCharArrayValue();

    void setCharSequenceValue(char[] newValue);

    void setCharSequenceValue(Field sourceField);

    void setCharSequenceValue(char[] newValue, int newValueLength);
}
