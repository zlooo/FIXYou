package io.github.zlooo.fixyou.commons.utils;

import io.github.zlooo.fixyou.commons.ReusableCharArray;
import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class FieldUtils {

    public static final int ASCII_ZERO_CODE = 48;
    private static final int ASCII_MINUS_CODE = 45; //- in ASCII
    private static final int MAX_DIGIT_NUMBER_HOLDABLE_BY_LONG = 19;
    private static final ThreadLocal<ReusableCharArray> REUSABLE_CHAR_ARRAY_THREAD_LOCAL = ThreadLocal.withInitial(ReusableCharArray::new);

    public static void writeEncoded(long valueToWrite, ByteBuf destinationBuffer) {
        if (valueToWrite == 0) {
            destinationBuffer.writeByte(ASCII_ZERO_CODE);
            return;
        }
        long value = valueToWrite;
        if (valueToWrite < 0) {
            destinationBuffer.writeByte(ASCII_MINUS_CODE);
            value = -1 * value;
        }
        int powerOfTenIndex = 0;
        for (; powerOfTenIndex < NumberConstants.POWERS_OF_TEN.length; powerOfTenIndex++) {
            if (NumberConstants.POWERS_OF_TEN[powerOfTenIndex] > value) {
                powerOfTenIndex--;
                break;
            }
        }
        for (; powerOfTenIndex >= 0; powerOfTenIndex--) {
            final long currentTenPowerValue = NumberConstants.POWERS_OF_TEN[powerOfTenIndex];
            final long digit = value / currentTenPowerValue;
            destinationBuffer.writeByte(ASCII_ZERO_CODE + (int) digit);
            value = value - (digit * currentTenPowerValue);
        }
    }

    public static void writeEncoded(long valueToWrite, ByteBuf destinationBuffer, int minLength) {
        long value = valueToWrite;
        final boolean writeMinus;
        if (valueToWrite < 0) {
            writeMinus = true;
            value = -1 * value;
        } else {
            writeMinus = false;
        }
        int powerOfTenIndex = 0;
        for (; powerOfTenIndex < NumberConstants.POWERS_OF_TEN.length; powerOfTenIndex++) {
            if (NumberConstants.POWERS_OF_TEN[powerOfTenIndex] > value) {
                break;
            }
        }
        int numberOfZeros = minLength - powerOfTenIndex;
        if (writeMinus) {
            numberOfZeros--;
        }
        for (int i = numberOfZeros; i > 0; i--) {
            destinationBuffer.writeByte(ASCII_ZERO_CODE);
        }
        if (writeMinus) {
            destinationBuffer.writeByte(ASCII_MINUS_CODE);
        }
        for (powerOfTenIndex--; powerOfTenIndex >= 0; powerOfTenIndex--) {
            final long currentTenPowerValue = NumberConstants.POWERS_OF_TEN[powerOfTenIndex];
            final long digit = value / currentTenPowerValue;
            destinationBuffer.writeByte(ASCII_ZERO_CODE + (int) digit);
            value = value - (digit * currentTenPowerValue);
        }
    }

    public static ReusableCharArray toCharSequence(long valueToWrite) {
        return toCharSequence(valueToWrite, 0);
    }

    /**
     * Almost pure copy of {@link Long#toString(long)}. The only difference is that this method returns {@link ReusableCharArray} and gives an ability to make underlying char
     * array larger if needed
     */
    public static ReusableCharArray toCharSequence(long valueToWrite, int additionalUnderlyingArrayLength) {
        final int size = (valueToWrite < 0) ? stringSize(-valueToWrite) + 1 : stringSize(valueToWrite);
        final char[] buf = new char[size + additionalUnderlyingArrayLength];
        getChars(valueToWrite, size, buf);
        //TODO make this poolable instead of using thread local, it's dangerous!!!!
        final ReusableCharArray reusableCharArray = REUSABLE_CHAR_ARRAY_THREAD_LOCAL.get();
        reusableCharArray.retain();
        return reusableCharArray.setCharArray(buf);
    }

    private static int stringSize(long x) {
        long p = 10;
        for (int i = 1; i < MAX_DIGIT_NUMBER_HOLDABLE_BY_LONG; i++) {
            if (x < p) {
                return i;
            }
            p = 10 * p;
        }
        return MAX_DIGIT_NUMBER_HOLDABLE_BY_LONG;
    }

    private static void getChars(long numberToGetCharsFrom, int index, char[] buf) {
        long q;
        int r;
        int charPos = index;
        char sign = 0;
        long i = numberToGetCharsFrom;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

        // Get 2 digits/iteration using longs until quotient fits into an int
        while (i > Integer.MAX_VALUE) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = (int) (i - ((q << 6) + (q << 5) + (q << 2)));
            i = q;
            buf[--charPos] = NumberConstants.DIGIT_ONES[r];
            buf[--charPos] = NumberConstants.DIGIT_TENS[r];
        }

        // Get 2 digits/iteration using ints
        int q2;
        int i2 = (int) i;
        while (i2 >= 65536) {
            q2 = i2 / 100;
            // really: r = i2 - (q * 100);
            r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
            i2 = q2;
            buf[--charPos] = NumberConstants.DIGIT_ONES[r];
            buf[--charPos] = NumberConstants.DIGIT_TENS[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i2 <= 65536, i2);
        for (; ; ) {
            q2 = (i2 * 52429) >>> (16 + 3);
            r = i2 - ((q2 << 3) + (q2 << 1));  // r = i2-(q2*10) ...
            buf[--charPos] = NumberConstants.DIGITS[r];
            i2 = q2;
            if (i2 == 0) {
                break;
            }
        }
        if (sign != 0) {
            buf[--charPos] = sign;
        }
    }
}