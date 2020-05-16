package pl.zlooo.fixyou.fix.commons.utils;

import pl.zlooo.fixyou.model.ApplicationVersionID;
import pl.zlooo.fixyou.model.FieldType;
import pl.zlooo.fixyou.model.FixSpec;

import javax.annotation.Nonnull;

public class FakeFixSpec implements FixSpec {

    private static final FieldNumberTypePair[] EMPTY_CHILD_PAIR_SPEC = new FieldNumberTypePair[]{};

    @Override
    public int[] getFieldsOrder() {
        return new int[0];
    }

    @Override
    public FieldType[] getTypes() {
        return new FieldType[0];
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
    public FieldNumberTypePair[] getChildPairSpec(int groupNumber) {
        return EMPTY_CHILD_PAIR_SPEC;
    }
}
