package io.github.zlooo.fixyou.parser

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.model.FieldType
import io.github.zlooo.fixyou.model.FixSpec

class TestSpec implements FixSpec {

    static int EMPTY_CHILD_PAIR_SPEC_FIELD_NUMBER = 666000
    static int NULL_CHILD_PAIR_SPEC_FIELD_NUMBER = 666001
    static int USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER = 3
    static int LONG_FIELD_NUMBER = 1
    static int BOOLEAN_FIELD_NUMBER = 2
    static INSTANCE = new TestSpec()

    @Override
    int[] getFieldsOrder() {
        return [LONG_FIELD_NUMBER, BOOLEAN_FIELD_NUMBER, USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER]
    }

    @Override
    FieldType[] getTypes() {
        return [FieldType.LONG, FieldType.BOOLEAN, FieldType.GROUP]
    }

    @Override
    char[][] getMessageTypes() {
        return [FixConstants.LOGON, ['D'] as char[]]
    }

    @Override
    int highestFieldNumber() {
        final int[] fieldsOrder = getFieldsOrder()
        Arrays.sort(fieldsOrder)
        return fieldsOrder[fieldsOrder.length - 1]
    }

    @Override
    ApplicationVersionID applicationVersionId() {
        return ApplicationVersionID.FIX50SP2
    }

    @Override
    FieldNumberTypePair[] getChildPairSpec(int groupNumber) {
        switch (groupNumber) {
            case EMPTY_CHILD_PAIR_SPEC_FIELD_NUMBER: return [] as FieldNumberTypePair[]
            case NULL_CHILD_PAIR_SPEC_FIELD_NUMBER: return null
            case USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER: return [new FieldNumberTypePair(FieldType.LONG, LONG_FIELD_NUMBER), new FieldNumberTypePair(FieldType.BOOLEAN, BOOLEAN_FIELD_NUMBER)] as FieldNumberTypePair[]
            default:
                return null
        }
    }
}
