package io.github.zlooo.fixyou.model;

import com.carrotsearch.hppcrt.IntByteMap;
import com.carrotsearch.hppcrt.IntObjectMap;

public interface ExtendedFixSpec extends FixSpec{
    IntObjectMap<FieldType> getFieldNumberToType();

    IntByteMap getHeaderFieldNumbers();
}
