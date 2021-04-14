package io.github.zlooo.fixyou.netty.handler.admin

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.model.DefaultExtendedFixSpec
import io.github.zlooo.fixyou.model.FieldType
import io.github.zlooo.fixyou.model.FixSpec
import io.github.zlooo.fixyou.parser.FixFieldsTypes

class TestSpec implements FixSpec {

    static INSTANCE = new DefaultExtendedFixSpec(new TestSpec())
    static TEST_DOUBLE_FIELD_NUMBER = 5001
    static TEST_CHAR_FIELD_NUMBER = 5002

    @Override
    int[] getHeaderFieldsOrder() {
        return [FixConstants.BEGIN_STRING_FIELD_NUMBER, FixConstants.BODY_LENGTH_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER,
                FixConstants.
                        SENDER_COMP_ID_FIELD_NUMBER, FixConstants.TARGET_COMP_ID_FIELD_NUMBER, FixConstants.SENDING_TIME_FIELD_NUMBER, FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER]
    }

    @Override
    FieldType[] getHeaderFieldTypes() {
        return [FixFieldsTypes.BEGIN_STRING, FixFieldsTypes.BODY_LENGTH, FixFieldsTypes.MESSAGE_TYPE, FixFieldsTypes.MESSAGE_SEQUENCE_NUMBER, FixFieldsTypes.POSSIBLE_DUPLICATE_FLAG, FixFieldsTypes.SENDER_COMP_ID, FixFieldsTypes.
                TARGET_COMP_ID, FixFieldsTypes.SENDING_TIME, FixFieldsTypes.ORIG_SENDING_TIME]
    }

    @Override
    int[] getBodyFieldsOrder() {
        return [FixConstants.TEST_REQ_ID_FIELD_NUMBER, FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.
                REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, FixConstants.GAP_FILL_FLAG_FIELD_NUMBER, FixConstants.TEXT_FIELD_NUMBER, FixConstants.
                RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER, FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, 453, 85, FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, TEST_DOUBLE_FIELD_NUMBER,
                TEST_CHAR_FIELD_NUMBER, FixConstants.CHECK_SUM_FIELD_NUMBER]
    }

    @Override
    FieldType[] getBodyFieldTypes() {
        return [FixFieldsTypes.TEST_REQ_ID, FixFieldsTypes.BEGIN_SEQUENCE_NUMBER, FixFieldsTypes.END_SEQUENCE_NUMBER, FixFieldsTypes.NEW_SEQUENCE_NUMBER, FixFieldsTypes.REFERENCED_SEQUENCE_NUMBER, FixFieldsTypes.ENCRYPT_METHOD,
                FixFieldsTypes.
                        HEARTBEAT_INTERVAL, FixFieldsTypes.GAP_FILL_FLAG, FixFieldsTypes.TEXT, FixFieldsTypes.RESET_SEQUENCE_NUMBER_FLAG, FixFieldsTypes.REFERENCED_TAG_ID, FixFieldsTypes.SESSION_REJECT_REASON, FieldType.GROUP, FieldType.
                GROUP, FixFieldsTypes.DEFAULT_APP_VERSION_ID, FieldType.DOUBLE, FieldType.CHAR, FixFieldsTypes.CHECK_SUM]
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
        switch (groupNumber) {
            case 453:
                return [new FieldNumberType(448, FieldType.CHAR_ARRAY)] as FieldNumberType[]
            case 85:
                return [new FieldNumberType(787, FieldType.CHAR), new FieldNumberType(781, FieldType.GROUP)] as FieldNumberType[]
            case 781:
                return [new FieldNumberType(782, FieldType.CHAR_ARRAY)] as FieldNumberType[]
            default:
                return [] as FieldNumberType[];
        }
    }
}
