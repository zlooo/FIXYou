package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.memory.MemoryConstants;
import lombok.experimental.UtilityClass;

@UtilityClass
class FieldConstants {

    static final byte BOOLEAN_FIELD_SIZE = MemoryConstants.BYTE_SIZE;
    static final byte CHAR_FIELD_SIZE = MemoryConstants.CHAR_SIZE;
    static final byte DOUBLE_FIELD_SIZE = MemoryConstants.LONG_SIZE + MemoryConstants.SHORT_SIZE;
    static final byte CHAR_SEQUENCE_LENGTH_SIZE = 2;
}
