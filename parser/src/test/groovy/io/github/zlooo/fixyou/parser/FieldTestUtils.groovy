package io.github.zlooo.fixyou.parser

class FieldTestUtils {

    public static final Comparator<? extends io.github.zlooo.fixyou.parser.model.AbstractField> FIELD_COMPARATOR = Comparator.comparing({ field -> field.number }).thenComparing({ field -> field.fieldData }, { buf1, buf2 -> buf1.compareTo(buf2) })
    public static final Comparator<? extends Map.Entry<? extends Integer, ? extends io.github.zlooo.fixyou.parser.model.AbstractField>> ENTRY_FIELD_COMPARATOR = Comparator.comparing({ entry -> entry.key }).thenComparing({ entry -> entry.value }, FIELD_COMPARATOR)
}
