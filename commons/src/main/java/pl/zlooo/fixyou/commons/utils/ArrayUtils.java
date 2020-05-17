package pl.zlooo.fixyou.commons.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ArrayUtils {

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

    /**
     * Simplified and unoptimized version of what can be found in JDK 9+
     */
    public static int compare(char[] a, char[] b) { //TODO either backport from JDK 9 or just optimize this method
        if (a == b) {
            return 0;
        }
        if (a == null || b == null) {
            return a == null ? -1 : 1;
        }

        final int i = mismatch(a, b, Math.min(a.length, b.length));
        final int result;
        if (i >= 0) {
            result = Character.compare(a[i], b[i]);
        } else {
            result = a.length - b.length;
        }
        return result;
    }

    private static int mismatch(char[] a, char[] b, int length) {
        int i = 0;
        for (; i < length; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        return -1;
    }
}
