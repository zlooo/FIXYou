package io.github.zlooo.fixyou.commons.utils;

import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DateUtils {

    private static final int ASCII_MINUS_CODE = 45; //- in ASCII
    private static final int ASCII_COMA_CODE = 46; //. in ASCII
    private static final int ASCII_COLON_CODE = 58; //: in ASCII
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
    private static final int LEAP_YEAR_MULTIPLE = 4;

    /**
     * This is <B>NOT</B> a general purpose method. It assumes fix timestamp format and <code>timestamp</code> cannot be earlier then 2020-01-01T00:00:00.000Z
     * @param timestamp epoch millis timestamp to write, cannot be earlier then 2020-01-01T00:00:00.000Z
     * @param destinationBuffer buffer that timestamp will be written into
     * @param withMillis should millis be included
     */
    //TODO remove checkstyle suppression on this file
    public static void writeTimestamp(long timestamp, ByteBuf destinationBuffer, boolean withMillis) {
        long remainingMillis = timestamp - BEGINNING_OF_MIN_YEAR;
        int year = MIN_YEAR;
        boolean isLeapYear = true;
        while (remainingMillis > MILLIS_IN_YEAR) {
            remainingMillis -= isLeapYear ? MILLIS_IN_LEAP_YEAR : MILLIS_IN_YEAR;
            year++;
            isLeapYear = year % LEAP_YEAR_MULTIPLE == 0;
        }
        int month = 1;
        while (remainingMillis > MILLIS_IN_MONTH_28) {
            if (month == 2) { //fuck you february
                if (isLeapYear) {
                    remainingMillis -= MILLIS_IN_MONTH_29;
                } else {
                    remainingMillis -= MILLIS_IN_MONTH_28;
                }
            } else if (month % 2 == 1) {
                remainingMillis -= MILLIS_IN_MONTH_31;
            } else {
                remainingMillis -= MILLIS_IN_MONTH_30;
            }
            month++;
        }
        int day = 1;
        while (remainingMillis > MILLIS_IN_DAY) {
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
        destinationBuffer.writeByte(ASCII_MINUS_CODE);
        FieldUtils.writeEncoded(hour, destinationBuffer, 2);
        destinationBuffer.writeByte(ASCII_COLON_CODE);
        FieldUtils.writeEncoded(minute, destinationBuffer, 2);
        destinationBuffer.writeByte(ASCII_COLON_CODE);
        FieldUtils.writeEncoded(second, destinationBuffer, 2);
        if (withMillis) {
            destinationBuffer.writeByte(ASCII_COMA_CODE);
            FieldUtils.writeEncoded(remainingMillis, destinationBuffer, 3);
        }
    }
}
