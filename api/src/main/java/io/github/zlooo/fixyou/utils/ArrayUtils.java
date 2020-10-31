package io.github.zlooo.fixyou.utils;

import lombok.experimental.UtilityClass;
import sun.misc.Unsafe;

@UtilityClass
public final class ArrayUtils {

    public static final int NOT_FOUND = -1;
    public static final int[] EMPTY_INT_ARRAY = new int[0];

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
        return (T) UnsafeAccessor.UNSAFE.getObject(array, (long) (Unsafe.ARRAY_OBJECT_BASE_OFFSET + (Unsafe.ARRAY_OBJECT_INDEX_SCALE * index)));
    }

    public static int getElementAt(int[] array, int index) {
        return UnsafeAccessor.UNSAFE.getInt(array, (long) (Unsafe.ARRAY_INT_BASE_OFFSET + (Unsafe.ARRAY_INT_INDEX_SCALE * index)));
    }

    public static char getElementAt(char[] array, int index) {
        return UnsafeAccessor.UNSAFE.getChar(array, (long) (Unsafe.ARRAY_CHAR_BASE_OFFSET + (Unsafe.ARRAY_CHAR_INDEX_SCALE * index)));
    }
}
