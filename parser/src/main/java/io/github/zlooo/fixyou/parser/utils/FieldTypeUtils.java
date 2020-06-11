package io.github.zlooo.fixyou.parser.utils;

import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.model.*;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;

@UtilityClass
public class FieldTypeUtils {

    @Nonnull
    public static <T extends AbstractField> T createField(@Nonnull FieldType fieldType, int fieldNumber, @Nonnull FixSpec fixSpec) {
        final T newField;
        switch (fieldType) {
            case LONG:
                newField = (T) new LongField(fieldNumber);
                break;
            case DOUBLE:
                newField = (T) new DoubleField(fieldNumber);
                break;
            case BOOLEAN:
                newField = (T) new BooleanField(fieldNumber);
                break;
            case CHAR_ARRAY:
                newField = (T) new CharSequenceField(fieldNumber);
                break;
            case CHAR:
                newField = (T) new CharField(fieldNumber);
                break;
            case GROUP:
                newField = (T) new GroupField(fieldNumber, fixSpec);
                break;
            case TIMESTAMP:
                newField = (T) new TimestampField(fieldNumber);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized field type " + fieldType);
        }
        return newField;
    }
}
