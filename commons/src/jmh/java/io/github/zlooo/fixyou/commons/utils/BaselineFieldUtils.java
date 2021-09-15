package io.github.zlooo.fixyou.commons.utils;

import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import lombok.experimental.UtilityClass;

@UtilityClass
class BaselineFieldUtils {

    static int writeEncoded(long valueToWrite, ByteBuf destinationBuffer) {
        if (valueToWrite == 0) {
            destinationBuffer.writeByte(AsciiCodes.ZERO);
            return AsciiCodes.ZERO;
        }
        long value = valueToWrite;
        int sumOfBytes = 0;
        if (valueToWrite < 0) {
            sumOfBytes += AsciiCodes.MINUS;
            destinationBuffer.writeByte(AsciiCodes.MINUS);
            value = -1 * value;
        }
        int powerOfTenIndex = 0;
        for (; powerOfTenIndex < NumberConstants.POWERS_OF_TEN.length; powerOfTenIndex++) {
            if (ArrayUtils.getElementAt(NumberConstants.POWERS_OF_TEN, powerOfTenIndex) > value) {
                powerOfTenIndex--;
                break;
            }
        }
        for (; powerOfTenIndex >= 0; powerOfTenIndex--) {
            final long currentTenPowerValue = ArrayUtils.getElementAt(NumberConstants.POWERS_OF_TEN, powerOfTenIndex);
            final long digit = value / currentTenPowerValue;
            final int digitInAscii = AsciiCodes.ZERO + (int) digit;
            sumOfBytes += digitInAscii;
            destinationBuffer.writeByte(digitInAscii);
            value = value - (digit * currentTenPowerValue);
        }
        return sumOfBytes;
    }
}
