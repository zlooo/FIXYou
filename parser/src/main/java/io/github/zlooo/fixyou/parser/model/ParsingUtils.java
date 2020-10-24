package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import lombok.experimental.UtilityClass;

import java.time.format.DateTimeFormatter;

@UtilityClass
public class ParsingUtils {

    public static final int RADIX = 10;
    public static final char FRACTION_SEPARATOR = '.';

    static void readChars(ByteBufComposer source, int srcIndex, int length, ByteBuf tempBuf, char[] destination) {
        tempBuf.clear();
        source.getBytes(srcIndex, length, tempBuf);
        for (int i = 0; i < length; i++) {
            destination[i] = AsciiString.b2c(tempBuf.getByte(i));
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

    static DateTimeFormatter chooseFormatter(int length) {
        if (FixConstants.UTC_TIMESTAMP_PATTERN.length() == length) {
            return FixConstants.UTC_TIMESTAMP_FORMATTER;
        } else {
            return FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER;
        }
    }
}
