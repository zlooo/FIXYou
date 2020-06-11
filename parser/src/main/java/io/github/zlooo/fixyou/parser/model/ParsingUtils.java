package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.internal.PlatformDependent;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ParsingUtils {

    public static final int RADIX = 10;

    static void readChars(ByteBuf source, int length, byte[] tempBuf, char[] destination) {
        source.readBytes(tempBuf, 0, length);
        for (int i = 0; i < length; i++) {
            destination[i] = AsciiString.b2c(PlatformDependent.getByte(tempBuf, i));
        }
    }

    public static int parseInteger(ByteBuf byteBuf, byte endIndicator) {
        int num = 0;
        boolean negative = false;
        while (true) {
            final byte b = byteBuf.readByte();
            if (b >= AsciiCodes.ZERO && b <= AsciiCodes.NINE) {
                num = num * ParsingUtils.RADIX + b - AsciiCodes.ZERO;
            } else if (b == AsciiCodes.MINUS) {
                negative = true;
            } else if (b == endIndicator) {
                break;
            }
        }
        return negative ? -num : num;
    }

    static long parseLong(ByteBuf byteBuf, byte endIndicator) {
        long num = 0;
        boolean negative = false;
        while (true) {
            final byte b = byteBuf.readByte();
            if (b >= AsciiCodes.ZERO && b <= AsciiCodes.NINE) {
                num = num * ParsingUtils.RADIX + b - AsciiCodes.ZERO;
            } else if (b == AsciiCodes.MINUS) {
                negative = true;
            } else if (b == endIndicator) {
                break;
            }
        }
        return negative ? -num : num;
    }
}
