package io.github.zlooo.fixyou


import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.model.FieldType
import io.github.zlooo.fixyou.model.FixSpec

class TestSpec implements FixSpec {

    static INSTANCE = new TestSpec()

    @Override
    int[] getHeaderFieldsOrder() {
        return [FixConstants.BEGIN_STRING_FIELD_NUMBER, FixConstants.BODY_LENGTH_FIELD_NUMBER]
    }

    @Override
    FieldType[] getHeaderFieldTypes() {
        return [FieldType.CHAR_ARRAY, FieldType.LONG]
    }

    @Override
    int[] getBodyFieldsOrder() {
        return [FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER,FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER]
    }

    @Override
    FieldType[] getBodyFieldTypes() {
        return [FieldType.LONG, FieldType.LONG, FieldType.LONG, FieldType.LONG]
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
    FieldNumberType[] getRepeatingGroupFieldNumbers(int groupNumber) {
        return [] as FieldNumberType[]
    }
}
