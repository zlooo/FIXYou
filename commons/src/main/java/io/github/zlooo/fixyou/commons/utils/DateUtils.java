package io.github.zlooo.fixyou.commons.utils;

import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;

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
    private static final long BEGINNING_OF_MIN_YEAR = 1577836800000L;
    private static final int MIN_YEAR = 2020;
    private static final int MOD_2_MASK = 1;
    private static final int MOD_4_MASK = 3;

    /**
     * This is <B>NOT</B> a general purpose method. It assumes fix timestamp format and <code>timestamp</code> cannot be earlier then 2020-01-01T00:00:00.000Z
     *
     * @param timestamp         epoch millis timestamp to write, cannot be earlier then 2020-01-01T00:00:00.000Z
     * @param destinationBuffer buffer that timestamp will be written into
     * @param withMillis        should millis be included
     */
    //TODO remove checkstyle suppression on this file
    public static void writeTimestamp(long timestamp, ByteBuf destinationBuffer, boolean withMillis) {
        long remainingMillis = timestamp - BEGINNING_OF_MIN_YEAR;
        int year = MIN_YEAR;
        boolean isLeapYear = true;
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
        FieldUtils.writeEncoded(year, destinationBuffer);
        FieldUtils.writeEncoded(month, destinationBuffer, 2);
        FieldUtils.writeEncoded(day, destinationBuffer, 2);
        destinationBuffer.writeByte(AsciiCodes.MINUS);
        FieldUtils.writeEncoded(hour, destinationBuffer, 2);
        destinationBuffer.writeByte(AsciiCodes.COLON);
        FieldUtils.writeEncoded(minute, destinationBuffer, 2);
        destinationBuffer.writeByte(AsciiCodes.COLON);
        FieldUtils.writeEncoded(second, destinationBuffer, 2);
        if (withMillis) {
            destinationBuffer.writeByte(AsciiCodes.DOT);
            FieldUtils.writeEncoded(remainingMillis, destinationBuffer, 3);
        }
    }
}
