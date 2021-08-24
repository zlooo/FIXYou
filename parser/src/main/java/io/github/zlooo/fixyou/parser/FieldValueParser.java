package io.github.zlooo.fixyou.parser;

import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FieldValueParser {
    public static final char FRACTION_SEPARATOR = '.';
    public static final int RADIX = 10;

    static boolean parseBoolean(ByteBuf source) {
        final byte valueToParse = source.readByte();
        switch (valueToParse) {
            case AsciiCodes.Y:
                return true;
            case AsciiCodes.N:
                return false;
            default:
                throw new IllegalArgumentException("Value " + valueToParse + " is unsupported in boolean field. Expecting either 'Y' or 'N'");
        }
    }

    static char parseChar(ByteBuf bytesToParse) {
        return AsciiString.b2c(bytesToParse.readByte());
    }

    static void setDoubleValuesFromAsciiByteBuf(ByteBuf asciiByteBuf, ValueHolders.IntHolder counter, ValueHolders.DecimalHolder valueHolder) {
        valueHolder.reset();
        counter.reset();
        final boolean negative = asciiByteBuf.getByte(0) == AsciiCodes.MINUS;
        if (negative) {
            counter.setValue(1);
        }

        final int length = asciiByteBuf.readableBytes();
        asciiByteBuf.forEachByte(counter.getValue(), negative ? length - 1 : length, byteRead -> {
            final char nextChar = AsciiString.b2c(byteRead);
            if (nextChar != FRACTION_SEPARATOR) {
                valueHolder.setUnscaledValue(valueHolder.getUnscaledValue() * RADIX + ((int) nextChar - AsciiCodes.ZERO));
            } else {
                valueHolder.setScale((short) (length - counter.getValue() - 1));
            }
            counter.getAndIncrement();
            return true;
        });
        if (negative) {
            valueHolder.setUnscaledValue(valueHolder.getUnscaledValue() * -1);
        }
    }

    static void parseInteger(ByteBuf byteBuf, ValueHolders.IntHolder valueHolder) {
        valueHolder.reset();
        byteBuf.forEachByte(byteRead -> {
            if (byteRead >= AsciiCodes.ZERO && byteRead <= AsciiCodes.NINE) {
                valueHolder.setValue(valueHolder.getValue() * RADIX + byteRead - AsciiCodes.ZERO);
            } else if (byteRead == AsciiCodes.MINUS) {
                valueHolder.setNegative(true);
            }
            return true;
        });
    }

    static long parseLong(ByteBuf byteBuf, ValueHolders.LongHolder valueHolder) {
        valueHolder.reset();
        byteBuf.forEachByte(byteRead -> {
            if (byteRead >= AsciiCodes.ZERO && byteRead <= AsciiCodes.NINE) {
                valueHolder.setValue(valueHolder.getValue() * RADIX + byteRead - AsciiCodes.ZERO);
            } else if (byteRead == AsciiCodes.MINUS) {
                valueHolder.setNegative(true);
            }
            return true;
        });
        return valueHolder.isNegative() ? -valueHolder.getValue() : valueHolder.getValue();
    }

}
