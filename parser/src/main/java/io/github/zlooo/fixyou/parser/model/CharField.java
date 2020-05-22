package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FieldType;
import io.netty.util.AsciiString;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
public class CharField extends AbstractField {

    private static final int FIELD_DATA_LENGTH = 1;
    private char value = Character.MIN_VALUE;

    public CharField(int number) {
        super(number, FIELD_DATA_LENGTH);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.CHAR;
    }

    public char getValue() {
        if (value == Character.MIN_VALUE) {
            fieldData.readerIndex(0);
            value = AsciiString.b2c(fieldData.readByte());
        }
        return value;
    }

    public void setValue(char value) {
        this.value = value;
        fieldData.clear().writeByte(AsciiString.c2b(value));
    }

    @Override
    protected void resetInnerState() {
        value = Character.MIN_VALUE;
    }
}
