package io.github.zlooo.fixyou.commons.utils;

import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class ListUtils {

    /**
     * An attempt and just an simple implementation of List.of() method from JDK 9+
     */
    @Nonnull
    public static <T> List<T> of(@Nonnull T... objects) {
        return Collections.unmodifiableList(Arrays.asList(objects));
    }
}
