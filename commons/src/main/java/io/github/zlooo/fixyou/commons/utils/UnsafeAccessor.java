package io.github.zlooo.fixyou.commons.utils;

import io.github.zlooo.fixyou.FIXYouException;
import lombok.experimental.UtilityClass;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

@UtilityClass
public class UnsafeAccessor {
    public static final Unsafe UNSAFE;
    public static final int ARRAY_BYTE_BASE_OFFSET;

    static {
        Unsafe unsafe = null;
        try {
            final PrivilegedExceptionAction<Unsafe> action =
                    () ->
                    {
                        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
                        f.setAccessible(true);

                        return (Unsafe) f.get(null);
                    };

            unsafe = AccessController.doPrivileged(action);
        } catch (final Exception ex) {
            throw new FIXYouException(ex);
        }

        UNSAFE = unsafe;
        ARRAY_BYTE_BASE_OFFSET = Unsafe.ARRAY_BYTE_BASE_OFFSET;
    }
}