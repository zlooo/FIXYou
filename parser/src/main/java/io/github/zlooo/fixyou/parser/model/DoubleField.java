package io.github.zlooo.fixyou.parser.model;

public interface DoubleField {
    long getDoubleUnscaledValue();

    short getScale();

    void setDoubleValue(long newValue, short newScale);
}
