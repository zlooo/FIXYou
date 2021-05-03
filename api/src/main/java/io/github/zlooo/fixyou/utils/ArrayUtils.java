package io.github.zlooo.fixyou.utils;

import io.github.zlooo.fixyou.model.FixSpec;
import lombok.experimental.UtilityClass;
import sun.misc.Unsafe;

@UtilityClass
public final class ArrayUtils {

    public static final int NOT_FOUND = -1;
    public static final int[] EMPTY_INT_ARRAY = new int[0];
    public static final long[] EMPTY_LONG_ARRAY = new long[0];
    public static final char[][] EMPTY_TWO_DIM_CHAR = new char[0][];
    public static final FixSpec.FieldNumberType[] EMPTY_FIELD_NUMBER_TYPE = new FixSpec.FieldNumberType[0];

    /**
     * Adds given element at given index moving following elements to the right if necessary. This method assumes sufficient capacity.
     */
    public static char[] insertElementAtIndex(char[] array, char elementToAdd, int index) {
        for (int i = array.length - 1; i > index; i--) {
            array[i] = array[i - 1];
        }
        array[index] = elementToAdd;
        return array;
    }

    public static int max(int... elements) {
        int result = elements[0];
        for (int i = 1; i < elements.length; i++) {
            if (elements[i] > result) {
                result = elements[i];
            }
        }
        return result;
    }

    public static <T> boolean contains(T[] array, T element) {
        for (final T arrayItem : array) {
            if (element.equals(arrayItem)) {
                return true;
            }
        }
        return false;
    }

    public static boolean equals(char[] array, CharSequence sequence) {
        final int length = array.length;
        if (length != sequence.length()) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (array[i] != sequence.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static int indexOf(int[] array, int element) {
        for (int i = 0; i < array.length; i++) {
            if (element == array[i]) {
                return i;
            }
        }
        return NOT_FOUND;
    }

    /**
     * Based on results I got from {@link io.github.zlooo.fixyou.commons.utils.ArrayDataGetPerformanceTest} it's faster to use this method than normal array[index]
     * ArrayDataGetPerformanceTest.unsafeReferenceGet - score 62,150
     * ArrayDataGetPerformanceTest.normalReferenceGet - score 45,399
     */
    public static <T> T getElementAt(T[] array, int index) {
        return (T) UnsafeAccessor.UNSAFE.getObject(array, Unsafe.ARRAY_OBJECT_BASE_OFFSET + (Unsafe.ARRAY_OBJECT_INDEX_SCALE * index));
    }

    public static int getElementAt(int[] array, int index) {
        return UnsafeAccessor.UNSAFE.getInt(array, Unsafe.ARRAY_INT_BASE_OFFSET + ((long) Unsafe.ARRAY_INT_INDEX_SCALE * index));
    }

    public static long getElementAt(long[] array, int index) {
        return UnsafeAccessor.UNSAFE.getLong(array, Unsafe.ARRAY_LONG_BASE_OFFSET + ((long) Unsafe.ARRAY_LONG_INDEX_SCALE * index));
    }

    public static char getElementAt(char[] array, int index) {
        return UnsafeAccessor.UNSAFE.getChar(array, Unsafe.ARRAY_CHAR_BASE_OFFSET + ((long) Unsafe.ARRAY_CHAR_INDEX_SCALE * index));
    }

    public static byte getElementAt(byte[] array, int index) {
        return UnsafeAccessor.UNSAFE.getByte(array, Unsafe.ARRAY_BYTE_BASE_OFFSET + ((long) Unsafe.ARRAY_BYTE_INDEX_SCALE * index));
    }

    public static <T> void putElementAt(T[] array, int index, T object) {
        UnsafeAccessor.UNSAFE.putObject(array, Unsafe.ARRAY_OBJECT_BASE_OFFSET + ((long) Unsafe.ARRAY_OBJECT_INDEX_SCALE * index), object);
    }

    public static void putElementAt(byte[] array, int index, byte byteToSet) {
        UnsafeAccessor.UNSAFE.putByte(array, Unsafe.ARRAY_BYTE_BASE_OFFSET + ((long) Unsafe.ARRAY_BYTE_INDEX_SCALE * index), byteToSet);
    }

    public static void putElementAt(int[] array, int index, int intToSet) {
        UnsafeAccessor.UNSAFE.putInt(array, Unsafe.ARRAY_INT_BASE_OFFSET + ((long) Unsafe.ARRAY_INT_INDEX_SCALE * index), intToSet);
    }

    public static void putElementAt(long[] array, int index, long longToSet) {
        UnsafeAccessor.UNSAFE.putLong(array, Unsafe.ARRAY_LONG_BASE_OFFSET + ((long) Unsafe.ARRAY_LONG_INDEX_SCALE * index), longToSet);
    }
}
