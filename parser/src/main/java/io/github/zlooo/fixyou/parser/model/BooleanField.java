package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FieldType;
import lombok.ToString;

@ToString(callSuper = true)
public class BooleanField extends AbstractField {

    private static final byte Y_IN_ASCII = 89;
    private static final byte N_IN_ASCII = 78;
    private static final int FIELD_DATA_LENGTH = 1;
    private boolean parsed;
    private boolean value;

    public BooleanField(int number) {
        super(number, FIELD_DATA_LENGTH);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.BOOLEAN;
    }

    public boolean getValue() {
        if (!parsed) {
            fieldData.readerIndex(0);
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
            parsed = true;
        }
        return value;
    }

    public void setValue(boolean state) {
        this.value = state;
        this.parsed = true;
        fieldData.clear().writeByte(state ? Y_IN_ASCII : N_IN_ASCII);
    }

    @Override
    protected void resetInnerState() {
        parsed = false;
    }
}
