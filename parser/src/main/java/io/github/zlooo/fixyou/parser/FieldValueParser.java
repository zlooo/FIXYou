package io.github.zlooo.fixyou.parser;

import com.carrotsearch.hppcrt.procedures.LongShortProcedure;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FieldValueParser {
    public static final char FRACTION_SEPARATOR = '.';
    static final int RADIX = 10;

    static int parseInteger(ByteBufComposer byteBuf, int srcIndex, byte endIndicator, boolean advanceReaderIndex) {
        int num = 0;
        boolean negative = false;
        int index = srcIndex;
        while (true) {
            final byte b = byteBuf.getByte(index++);
            if (b >= AsciiCodes.ZERO && b <= AsciiCodes.NINE) {
                num = num * RADIX + b - AsciiCodes.ZERO;
            } else if (b == AsciiCodes.MINUS) {
                negative = true;
            } else if (b == endIndicator) {
                break;
            }
        }
        if (advanceReaderIndex) {
            byteBuf.readerIndex(index);
        }
        return negative ? -num : num;
    }

    static boolean parseBoolean(ByteBufComposer source, int srcIndex) {
        final byte valueToParse = source.getByte(srcIndex);
        switch (valueToParse) {
            case AsciiCodes.Y:
                return true;
            case AsciiCodes.N:
                return false;
            default:
                throw new IllegalArgumentException("Value " + valueToParse + " is unsupported in boolean field. Expecting either 'Y' or 'N'");
        }
    }

    static char parseChar(ByteBufComposer bytesToParse, int srcIndex) {
        return AsciiString.b2c(bytesToParse.getByte(srcIndex));
    }

    static void readChars(ByteBufComposer source, int srcIndex, int length, ByteBuf tempBuffer, char[] destination) {
        tempBuffer.clear();
        source.getBytes(srcIndex, length, tempBuffer);
        for (int i = 0; i < length; i++) {
            destination[i] = AsciiString.b2c(tempBuffer.getByte(i));
        }
    }

    static void setDoubleValuesFromByteBufComposer(ByteBufComposer source, int srcIndex, int length, ByteBuf tempBuffer, char[] tempCharBuffer, LongShortProcedure valueConsumer) {
        //TODO after JMH check in LongField is done and you're right, check this one as well
        readChars(source, srcIndex, length, tempBuffer, tempCharBuffer);
        final boolean negative = tempCharBuffer[0] == '-';
        long unscaledValue = 0;
        short scale = 0;
        for (int i = negative ? 1 : 0; i < length; i++) {
            final char nextChar = tempCharBuffer[i];
            if (nextChar != FRACTION_SEPARATOR) {
                unscaledValue = unscaledValue * RADIX + ((int) nextChar - AsciiCodes.ZERO);
            } else {
                scale = (short) (length - i - 1);
            }
        }
        if (negative) {
            unscaledValue *= -1;
        }
        valueConsumer.apply(unscaledValue, scale);
    }

    static long parseLong(ByteBufComposer byteBuf, int srcIndex, byte endIndicator) {
        long num = 0;
        boolean negative = false;
        int index = srcIndex;
        while (true) {
            final byte b = byteBuf.getByte(index++);
            if (b >= AsciiCodes.ZERO && b <= AsciiCodes.NINE) {
                num = num * RADIX + b - AsciiCodes.ZERO;
            } else if (b == AsciiCodes.MINUS) {
                negative = true;
            } else if (b == endIndicator) {
                break;
            }
        }
        return negative ? -num : num;
    }
}
