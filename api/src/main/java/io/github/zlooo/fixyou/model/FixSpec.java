package io.github.zlooo.fixyou.model;

import io.github.zlooo.fixyou.utils.ArrayUtils;

import javax.annotation.Nonnull;

public interface FixSpec {

    @Nonnull
    int[] getFieldsOrder();

    @Nonnull
    char[][] getMessageTypes();

    @Nonnull
    ApplicationVersionID applicationVersionId();

    @Nonnull
    int[] getRepeatingGroupFieldNumbers(int groupNumber);

    default int highestFieldNumber(){
        return ArrayUtils.max(getFieldsOrder());
    }
}
