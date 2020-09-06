package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class BooleanField extends AbstractField {

    private boolean value;
    private boolean parsed;

    public BooleanField(int number) {
        super(number);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.BOOLEAN;
    }

    public boolean getValue() {
        if (!parsed && valueSet) {
            final byte byteRead = fieldData.getByte(startIndex);
            switch (byteRead) {
                case AsciiCodes.Y:
                    value = true;
                    break;
                case AsciiCodes.N:
                    value = false;
                    break;
                default:
                    throw new IllegalArgumentException("Value " + byteRead + " is unsupported in boolean field. Expecting either 'Y' or 'N'");
            }
            parsed = true;
        }
        return value;
    }

    public void setValue(boolean state) {
        this.value = state;
        this.parsed = true;
        this.valueSet = true;
    }

    @Override
    public int appendByteBufWithValue(ByteBuf out) {
        if (value) {
            out.writeByte(AsciiCodes.Y);
            return AsciiCodes.Y;
        } else {
            out.writeByte(AsciiCodes.N);
            return AsciiCodes.N;
        }
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    protected void resetInnerState() {
        parsed = false;
    }
}
