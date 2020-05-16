package pl.zlooo.fixyou.commons.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;

@UtilityClass
public final class ReflectionUtils {

    @SneakyThrows
    public static void setFinalField(Object object, String fieldName, Object fieldValue) {
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
        field.setAccessible(true);
        field.set(object, fieldValue);
        field.setAccessible(false);
    }
}
