package io.github.zlooo.fixyou.parser.model;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FixMessageRepeatingGroupUtils {

    public static final int REPEATING_GROUP_NUMBER_KEY_SHIFT_NUMBER = 24;
    public static final int GROUP_NUMBER_MASK = 0x00ffffff;
    public static final long INDEX_NUMBER_LONG_WIDENING_MASK = 0x00000000000000ffL;
    public static final int INDEX_NUMBER_INT_WIDENING_MASK = 0x000000ff;
    private static final int REPEATING_GROUP_INDEX_KEY_SHIFT_NUMBER = 8;

    /**
     * Key structure 1 byte parent group index, 3 bytes parent group number, 1 byte repetition index, 3 bytes field number
     *
     * @param parentGroupRepetitionIndex first constituent of key bytes 63-56
     * @param groupNumber                second constituent of key bytes 55-32
     * @param repetitionIndex            third constituent of key bytes 31-24
     * @param fieldNumber                forth constituent of key bytes 23-0
     * @return
     */
    static long repeatingGroupKey(byte parentGroupRepetitionIndex, int groupNumber, byte repetitionIndex, int fieldNumber) {
        final long groupPart = ((parentGroupRepetitionIndex & INDEX_NUMBER_LONG_WIDENING_MASK) << REPEATING_GROUP_NUMBER_KEY_SHIFT_NUMBER) | groupNumber;
        return (((groupPart << REPEATING_GROUP_INDEX_KEY_SHIFT_NUMBER) | (repetitionIndex & INDEX_NUMBER_LONG_WIDENING_MASK)) << REPEATING_GROUP_NUMBER_KEY_SHIFT_NUMBER) | fieldNumber;
    }

    public static int groupIndex(byte repetitionIndex, int groupNumber) {
        return ((repetitionIndex & INDEX_NUMBER_INT_WIDENING_MASK) << REPEATING_GROUP_NUMBER_KEY_SHIFT_NUMBER) | groupNumber;
    }
}
