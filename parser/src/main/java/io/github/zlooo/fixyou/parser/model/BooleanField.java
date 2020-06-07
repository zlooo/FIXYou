package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FieldType;
import lombok.ToString;

@ToString(callSuper = true)
public class BooleanField extends AbstractField {

    private static final byte Y_IN_ASCII = 89;
    private static final byte N_IN_ASCII = 78;
    private boolean value;

    public BooleanField(int number) {
        super(number);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.BOOLEAN;
    }

    public boolean getValue() {
        if (!valueSet) {
            fieldData.readerIndex(startIndex);
            switch (fieldData.readByte()) {
                case Y_IN_ASCII:
                    value = true;
                    break;
                case N_IN_ASCII:
                    value = false;
                    break;
                default:
                    throw new IllegalArgumentException("Value " + fieldData.getByte(0) + " is unsupported in boolean field. Expecting either 'Y' or 'N'");
            }
            valueSet = true;
        }
        return value;
    }

    public void setValue(boolean state) {
        this.value = state;
        this.valueSet = true;
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    protected void resetInnerState() {
        // nothing to do
    }
}
