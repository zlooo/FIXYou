package pl.zlooo.fixyou.parser

import pl.zlooo.fixyou.parser.model.AbstractField

class FieldTestUtils {

    public static final Comparator<? extends AbstractField> FIELD_COMPARATOR = Comparator.comparing({ field -> field.number }).thenComparing({ field -> field.fieldData }, { buf1, buf2 -> buf1.compareTo(buf2) })
    public static final Comparator<? extends Map.Entry<? extends Integer, ? extends AbstractField>> ENTRY_FIELD_COMPARATOR = Comparator.comparing({ entry -> entry.key }).thenComparing({ entry -> entry.value }, FIELD_COMPARATOR)
}
