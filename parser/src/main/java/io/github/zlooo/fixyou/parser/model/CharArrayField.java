package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.util.AsciiString;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
@ToString(callSuper = true)
public class CharArrayField extends AbstractField {

    private char[] value;

    public CharArrayField(int number) {
        super(number, DefaultConfiguration.FIELD_BUFFER_SIZE);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.CHAR_ARRAY;
    }

    public char[] getValue() {
        if (value == null) {
            fieldData.readerIndex(0);
            value = ((AsciiString) fieldData.readCharSequence(fieldData.writerIndex(), StandardCharsets.US_ASCII)).toCharArray();
        }
        return value;
    }

    public void setValue(char[] value) {
        this.value = value;
        fieldData.clear();
        for (final char singleCharacter : value) {
            fieldData.writeByte(AsciiString.c2b(singleCharacter));
        }
    }

    @Override
    protected void resetInnerState() {
        value = null;
    }
}
