package io.github.zlooo.fixyou.parser;

import io.github.zlooo.fixyou.model.ApplicationVersionID;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.utils.ArrayUtils;

import javax.annotation.Nonnull;

public class FakeFixSpec implements FixSpec {

    public static final FakeFixSpec INSTANCE = new FakeFixSpec();
    private static final FieldType[] EMPTY_FIELD_TYPE = new FieldType[0];

    @Nonnull
    @Override
    public int[] getHeaderFieldsOrder() {
        return ArrayUtils.EMPTY_INT_ARRAY;
    }

    @Nonnull
    @Override
    public FieldType[] getHeaderFieldTypes() {
        return EMPTY_FIELD_TYPE;
    }

    @Nonnull
    @Override
    public int[] getBodyFieldsOrder() {
        return ArrayUtils.EMPTY_INT_ARRAY;
    }

    @Nonnull
    @Override
    public FieldType[] getBodyFieldTypes() {
        return EMPTY_FIELD_TYPE;
    }

    @Nonnull
    @Override
    public char[][] getMessageTypes() {
        return ArrayUtils.EMPTY_TWO_DIM_CHAR;
    }

    @Override
    public ApplicationVersionID applicationVersionId() {
        return ApplicationVersionID.FIX50SP2;
    }

    @Override
    public FieldNumberType[] getRepeatingGroupFieldNumbers(int groupNumber) {
        return ArrayUtils.EMPTY_FIELD_NUMBER_TYPE;
    }
}
