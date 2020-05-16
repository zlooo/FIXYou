package pl.zlooo.fixyou.parser.utils;

import lombok.experimental.UtilityClass;
import pl.zlooo.fixyou.model.FieldType;
import pl.zlooo.fixyou.model.FixSpec;
import pl.zlooo.fixyou.parser.model.*;

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
                newField = (T) new CharArrayField(fieldNumber);
                break;
            case CHAR:
                newField = (T) new CharField(fieldNumber);
                break;
            case GROUP:
                newField = (T) new GroupField(fieldNumber, fixSpec);
                break;
            default:
                throw new IllegalArgumentException("Unrecognized field type " + fieldType);
        }
        return newField;
    }
}
