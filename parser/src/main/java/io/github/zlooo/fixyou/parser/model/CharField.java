package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FieldType;
import io.netty.util.AsciiString;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
public class CharField extends AbstractField {

    private char value = Character.MIN_VALUE;

    public CharField(int number) {
        super(number);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.CHAR;
    }

    public char getValue() {
        if (value == Character.MIN_VALUE) {
            fieldData.readerIndex(startIndex);
            value = AsciiString.b2c(fieldData.readByte());
        }
        return value;
    }

    public void setValue(char value) {
        this.value = value;
        this.valueSet = true;
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
