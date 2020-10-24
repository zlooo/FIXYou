package io.github.zlooo.fixyou.parser;

import io.github.zlooo.fixyou.model.ApplicationVersionID;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.utils.ArrayUtils;

import javax.annotation.Nonnull;

public class FakeFixSpec implements FixSpec {

    public static final FakeFixSpec INSTANCE = new FakeFixSpec();

    @Override
    public int[] getFieldsOrder() {
        return new int[0];
    }

    @Nonnull
    @Override
    public char[][] getMessageTypes() {
        return new char[0][];
    }

    @Override
    public int highestFieldNumber() {
        return 0;
    }

    @Override
    public ApplicationVersionID applicationVersionId() {
        return ApplicationVersionID.FIX50SP2;
    }

    @Override
    public int[] getRepeatingGroupFieldNumbers(int groupNumber) {
        return ArrayUtils.EMPTY_INT_ARRAY;
    }
}
