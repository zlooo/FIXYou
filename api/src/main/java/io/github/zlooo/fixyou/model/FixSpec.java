package io.github.zlooo.fixyou.model;

import lombok.Value;

import javax.annotation.Nonnull;

public interface FixSpec {

    @Nonnull
    int[] getHeaderFieldsOrder();

    @Nonnull
    FieldType[] getHeaderFieldTypes();

    @Nonnull
    int[] getBodyFieldsOrder();

    @Nonnull
    FieldType[] getBodyFieldTypes();

    @Nonnull
    char[][] getMessageTypes();

    @Nonnull
    ApplicationVersionID applicationVersionId();

    @Nonnull
    FieldNumberType[] getRepeatingGroupFieldNumbers(int groupNumber);

    @Value
    class FieldNumberType{
        private int number;
        private FieldType type;
    }
}
