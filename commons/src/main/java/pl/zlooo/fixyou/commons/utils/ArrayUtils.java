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
}
