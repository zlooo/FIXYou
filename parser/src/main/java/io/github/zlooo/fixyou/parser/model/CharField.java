package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FieldType;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class CharField extends AbstractField {

    public static final char DEFAULT_VALUE = Character.MIN_VALUE;
    private char value = DEFAULT_VALUE;

    public CharField(int number) {
        super(number);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.CHAR;
    }

    public char getValue() {
        if (value == Character.MIN_VALUE && valueSet) {
            value = AsciiString.b2c(fieldData.getByte(startIndex));
        }
        return value;
    }

    public void setValue(char value) {
        this.value = value;
        this.valueSet = true;
    }

    @Override
    public void appendByteBufWithValue(ByteBuf out) {
        out.writeByte(AsciiString.c2b(value));
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    protected void resetInnerState() {
        value = Character.MIN_VALUE;
    }
}
