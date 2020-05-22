package io.github.zlooo.fixyou.model;

import lombok.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface FixSpec {

    int[] getFieldsOrder();

    @Nonnull
    FieldType[] getTypes();

    @Nonnull
    char[][] getMessageTypes();

    int highestFieldNumber();

    @Nonnull
    ApplicationVersionID applicationVersionId();

    @Nullable
    FieldNumberTypePair[] getChildPairSpec(int groupNumber);

    @Getter
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    final class FieldNumberTypePair {
        private @NonNull FieldType fieldType;
        private int fieldNumber;
    }
}
