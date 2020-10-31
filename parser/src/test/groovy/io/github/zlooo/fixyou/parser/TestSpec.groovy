package io.github.zlooo.fixyou.parser

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.model.FixSpec

class TestSpec implements FixSpec {

    static int EMPTY_CHILD_PAIR_SPEC_FIELD_NUMBER = 666000
    static int NULL_CHILD_PAIR_SPEC_FIELD_NUMBER = 666001
    static int USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER = 3
    static int LONG_FIELD_NUMBER = 1
    static int BOOLEAN_FIELD_NUMBER = 2
    static int BEGIN_STRING_FIELD_NUMBER = 8
    static INSTANCE = new TestSpec()

    @Override
    int[] getFieldsOrder() {
        return [LONG_FIELD_NUMBER, BOOLEAN_FIELD_NUMBER, USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER, BEGIN_STRING_FIELD_NUMBER]
    }

    @Override
    char[][] getMessageTypes() {
        return [FixConstants.LOGON, ['D'] as char[]]
    }

    @Override
    ApplicationVersionID applicationVersionId() {
        return ApplicationVersionID.FIX50SP2
    }

    @Override
    int[] getRepeatingGroupFieldNumbers(int groupNumber) {
        switch (groupNumber) {
            case EMPTY_CHILD_PAIR_SPEC_FIELD_NUMBER: return [] as int[]
            case USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER: return [LONG_FIELD_NUMBER, BOOLEAN_FIELD_NUMBER] as int[]
            default:
                throw new IllegalArgumentException("Only 2 group numbers are supported, empty - $EMPTY_CHILD_PAIR_SPEC_FIELD_NUMBER and the one containing 2 fields - $USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER")
        }
    }
}
