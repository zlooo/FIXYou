package io.github.zlooo.fixyou.commons.utils;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import lombok.experimental.UtilityClass;

import java.time.format.DateTimeFormatter;

/**
 * Yeah, this class looks like shit, but at least it's faster than {@link DateTimeFormatter}. See
 * <a href="https://github.com/zlooo/FIXYou/blob/master/commons/src/jmh/java/io/github/zlooo/fixyou/commons/utils/DateUtilsPerformanceTest.java">DateUtilsPerformanceTest</a> for details.
 * However probably by making it even uglier it'll also be faster ;)
 */
@UtilityClass
public class DateUtils {

    private static final long MILLIS_IN_SECOND = 1000L;
    private static final long MILLIS_IN_MINUTE = 60000L;
    private static final long MILLIS_IN_HOUR = 3600000L;
    private static final long MILLIS_IN_DAY = 86400000L;
    private static final long MILLIS_IN_MONTH_28 = 2419200000L;
    private static final long MILLIS_IN_MONTH_29 = 2505600000L;
    private static final long MILLIS_IN_MONTH_30 = 2592000000L;
    private static final long MILLIS_IN_MONTH_31 = 2678400000L;
    private static final long MILLIS_IN_YEAR = 31536000000L;
    private static final long MILLIS_IN_LEAP_YEAR = 31622400000L;
    private static final long BEGINNING_OF_MIN_YEAR = 1609459200000L;
    private static final int MIN_YEAR = 2021;
    private static final int MOD_4_MASK = 3;
    private static final int RADIX = 10;
    static final long RADIX_2 = 100;

    /**
     * This is <B>NOT</B> a general purpose method. It assumes fix timestamp format and <code>timestamp</code> cannot be earlier then 2021-01-01T00:00:00.000Z
     *
     * @param timestamp         epoch millis timestamp to write, cannot be earlier then 2021-01-01T00:00:00.000Z
     * @param destinationBuffer buffer that timestamp will be written into
     * @param withMillis        should millis be included
     */
    //TODO remove checkstyle suppression on this file
    public static int writeTimestamp(long timestamp, ByteBuf destinationBuffer, boolean withMillis) {
        long remainingMillis = timestamp - BEGINNING_OF_MIN_YEAR;
        int year = MIN_YEAR;
        boolean isLeapYear = false;
        while (remainingMillis > MILLIS_IN_YEAR) {
            remainingMillis -= isLeapYear ? MILLIS_IN_LEAP_YEAR : MILLIS_IN_YEAR;
            year++;
            isLeapYear = (year & MOD_4_MASK) == 0;
        }
        int month = 1;
        long monthMillis = MILLIS_IN_MONTH_31;
        while (remainingMillis >= monthMillis) {
            remainingMillis -= monthMillis;
            month++;
            switch (month) {
                case 1:
                case 3:
                case 5:
                case 7:
                case 8://seriously? Gregorian calendar is so fucked up
                case 10:
                case 12:
                    monthMillis = MILLIS_IN_MONTH_31;
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    monthMillis = MILLIS_IN_MONTH_30;
                    break;
                case 2://fuck you february
                    if (isLeapYear) {
                        monthMillis = MILLIS_IN_MONTH_29;
                    } else {
                        monthMillis = MILLIS_IN_MONTH_28;
                    }
                    break;
                default:
                    throw new IllegalArgumentException(month + "? What a weird month that is");
            }
        }
        int day = 1;
        while (remainingMillis >= MILLIS_IN_DAY) {
            remainingMillis -= MILLIS_IN_DAY;
            day++;
        }
        int hour = 0;
        while (remainingMillis >= MILLIS_IN_HOUR) {
            remainingMillis -= MILLIS_IN_HOUR;
            hour++;
        }
        int minute = 0;
        while (remainingMillis >= MILLIS_IN_MINUTE) {
            remainingMillis -= MILLIS_IN_MINUTE;
            minute++;
        }
        int second = 0;
        while (remainingMillis >= MILLIS_IN_SECOND) {
            remainingMillis -= MILLIS_IN_SECOND;
            second++;
        }
        int sumOfBytes = FieldUtils.writeEncoded((((year * RADIX_2) + month) * RADIX_2) + day, destinationBuffer);
        sumOfBytes += AsciiCodes.MINUS;
        destinationBuffer.writeByte(AsciiCodes.MINUS);
        sumOfBytes += FieldUtils.writeEncoded(hour, destinationBuffer, 2);
        sumOfBytes += AsciiCodes.COLON;
        destinationBuffer.writeByte(AsciiCodes.COLON);
        sumOfBytes += FieldUtils.writeEncoded(minute, destinationBuffer, 2);
        sumOfBytes += AsciiCodes.COLON;
        destinationBuffer.writeByte(AsciiCodes.COLON);
        sumOfBytes += FieldUtils.writeEncoded(second, destinationBuffer, 2);
        if (withMillis) {
            sumOfBytes += AsciiCodes.DOT;
            destinationBuffer.writeByte(AsciiCodes.DOT);
            sumOfBytes += FieldUtils.writeEncoded(remainingMillis, destinationBuffer, 3);
        }
        return sumOfBytes;
    }

    public static DateTimeFormatter chooseFormatter(int length) {
        if (FixConstants.UTC_TIMESTAMP_PATTERN.length() == length) {
            return FixConstants.UTC_TIMESTAMP_FORMATTER;
        } else {
            return FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER;
        }
    }

    public static long parseTimestamp(ByteBufComposer byteBuf, int srcIndex, int length, TimestampParser timestampParser) {
        final ByteBuf bytes = Unpooled.buffer(length, length);
        byteBuf.getBytes(srcIndex, length, bytes);
        bytes.forEachByte(timestampParser);
        return timestampParser.result;
    }

    public static final class TimestampParser implements ByteProcessor {
        private long result = BEGINNING_OF_MIN_YEAR;
        private int temp = 0;
        private byte counter = 1;
        private boolean isLeapYear = false;

        public void reset() {
            result = BEGINNING_OF_MIN_YEAR;
            temp = 0;
            counter = 1;
            isLeapYear = false;
        }

        //yyyyMMdd-HH:mm:ss.SSS
        //123456789       17  21
        @Override
        public boolean process(byte value) throws Exception {
            switch (counter++) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 6:
                case 8:
                case 11:
                case 14:
                case 20:
                    temp = temp * RADIX + value - AsciiCodes.ZERO;
                    break;
                case 5:
                    isLeapYear = (temp & MOD_4_MASK) == 0;
                    for (int i = MIN_YEAR; i < temp; i++) {
                        result += (i & MOD_4_MASK) == 0 ? MILLIS_IN_LEAP_YEAR : MILLIS_IN_YEAR;
                    }
                    temp = value - AsciiCodes.ZERO;
                    break;
                case 7:
                    while (temp > 1) {
                        temp--;
                        switch (temp) {
                            case 1:
                            case 3:
                            case 5:
                            case 7:
                            case 8://seriously? Gregorian calendar is so fucked up
                            case 10:
                            case 12:
                                result += MILLIS_IN_MONTH_31;
                                break;
                            case 4:
                            case 6:
                            case 9:
                            case 11:
                                result += MILLIS_IN_MONTH_30;
                                break;
                            case 2://fuck you february
                                if (isLeapYear) {
                                    result += MILLIS_IN_MONTH_29;
                                } else {
                                    result += MILLIS_IN_MONTH_28;
                                }
                                break;
                            default:
                                throw new IllegalArgumentException(temp + "? What a weird month that is");
                        }
                    }
                    temp = value - AsciiCodes.ZERO;
                    break;
                case 9:
                case 12:
                case 15:
                case 18:
                    break;
                case 10:
                    result += (temp - 1) * MILLIS_IN_DAY;
                    temp = value - AsciiCodes.ZERO;
                    break;
                case 13:
                    result += temp * MILLIS_IN_HOUR;
                    temp = value - AsciiCodes.ZERO;
                    break;
                case 16:
                    result += temp * MILLIS_IN_MINUTE;
                    temp = value - AsciiCodes.ZERO;
                    break;
                case 17:
                    result += (((long) temp * RADIX) + value - AsciiCodes.ZERO) * MILLIS_IN_SECOND;
                    break;
                case 19:
                    temp = value - AsciiCodes.ZERO;
                    break;
                case 21:
                    result += ((long) temp * RADIX) + value - AsciiCodes.ZERO;
                    break;
                default:
                    throw new IndexOutOfBoundsException("Index for timestamp is out of range. It can't be larger than 21 and yet it's " + counter);
            }
            return true;
        }
    }
}
