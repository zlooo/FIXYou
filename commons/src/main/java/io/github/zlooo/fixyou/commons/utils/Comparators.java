package io.github.zlooo.fixyou.commons.utils;

import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;

@UtilityClass
public class Comparators {

    /**
     * Copy from JDK 11
     */
    public static int compare(@Nonnull CharSequence cs1, @Nonnull CharSequence cs2) {
        if (cs1.getClass() == cs2.getClass() && cs1 instanceof Comparable) {
            return ((Comparable<Object>) cs1).compareTo(cs2);
        }

        for (int i = 0, len = Math.min(cs1.length(), cs2.length()); i < len; i++) {
            final char a = cs1.charAt(i);
            final char b = cs2.charAt(i);
            if (a != b) {
                return a - b;
            }
        }

        return cs1.length() - cs2.length();
    }
}
