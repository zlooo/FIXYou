package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import io.netty.util.internal.PlatformDependent;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ParsingUtils {

    public static final int RADIX = 10;

    static void readChars(ByteBufComposer source, int srcIndex, int length, byte[] tempBuf, char[] destination) {
        source.getBytes(srcIndex, length, tempBuf);
        for (int i = 0; i < length; i++) {
            destination[i] = AsciiString.b2c(PlatformDependent.getByte(tempBuf, i)); //TODO just out of curiosity JMH this and see if it's faster than ordinary for loop
        }
    }

    public static int parseInteger(ByteBufComposer byteBuf, int srcIndex, byte endIndicator, boolean advanceReaderIndex) {
        int num = 0;
        boolean negative = false;
        int index = srcIndex;
        while (true) {
            final byte b = byteBuf.getByte(index++);
            if (b >= AsciiCodes.ZERO && b <= AsciiCodes.NINE) {
                num = num * ParsingUtils.RADIX + b - AsciiCodes.ZERO;
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

    static long parseLong(ByteBufComposer byteBuf, int srcIndex, byte endIndicator) {
        long num = 0;
        boolean negative = false;
        int index = srcIndex;
        while (true) {
            final byte b = byteBuf.getByte(index++);
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
