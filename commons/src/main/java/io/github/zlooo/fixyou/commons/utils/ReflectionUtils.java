package io.github.zlooo.fixyou.commons.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;

@UtilityClass
public final class ReflectionUtils {

    @SneakyThrows
    public static void setFinalField(Object object, String fieldName, Object fieldValue) {
        final Field field = findField(object, fieldName);
        setFinalField(object, field, fieldValue);
    }

    @SneakyThrows
    public static void setFinalField(Object object, Field field, Object fieldValue) {
        field.setAccessible(true);
        field.set(object, fieldValue);
        field.setAccessible(false);
    }

    public static Field findField(Object object, String fieldName) throws NoSuchFieldException {
        Field field = null;
        Class<?> classToSearch = object.getClass();
        while (field == null && classToSearch != Object.class) {
            try {
                field = classToSearch.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                //nothing to do
            }
            classToSearch = classToSearch.getSuperclass();
        }
        if (field == null) {
            throw new NoSuchFieldException("Could not find field " + fieldName + " in class " + object.getClass() + " and it's super classes");
        }
        return field;
    }

    @SneakyThrows
    public static <T> T getFieldValue(Object object, String fieldName, Class<T> fieldClass) {
        final Field field = findField(object, fieldName);
        final boolean accessible = field.isAccessible();
        try {
            field.setAccessible(true);
            return (T) field.get(object);
        } finally {
            field.setAccessible(accessible);
        }
    }
}
