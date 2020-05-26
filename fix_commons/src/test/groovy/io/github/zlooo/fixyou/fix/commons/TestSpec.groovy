package io.github.zlooo.fixyou.fix.commons

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.model.FieldType
import io.github.zlooo.fixyou.model.FixSpec
import io.github.zlooo.fixyou.parser.FixFieldsTypes

class TestSpec implements FixSpec {

    static INSTANCE = new TestSpec()

    @Override
    int[] getFieldsOrder() {
        return [FixConstants.BEGIN_STRING_FIELD_NUMBER, FixConstants.BODY_LENGTH_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER,
                FixConstants.
                        SENDER_COMP_ID_FIELD_NUMBER, FixConstants.TARGET_COMP_ID_FIELD_NUMBER, FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER,
                FixConstants.
                        REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, FixConstants.GAP_FILL_FLAG_FIELD_NUMBER, FixConstants.
                RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER, FixConstants.TEXT_FIELD_NUMBER, FixConstants.TEST_REQ_ID_FIELD_NUMBER, FixConstants.
                REFERENCED_TAG_ID_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, FixConstants.CHECK_SUM_FIELD_NUMBER]
    }

    @Override
    FieldType[] getTypes() {
        return [FixFieldsTypes.BEGIN_STRING, FixFieldsTypes.BODY_LENGTH, FixFieldsTypes.MESSAGE_TYPE, FixFieldsTypes.MESSAGE_SEQUENCE_NUMBER, FixFieldsTypes.POSS_DUP_FLAG, FixFieldsTypes.SENDER_COMP_ID, FixFieldsTypes.TARGET_COMP_ID,
                FixFieldsTypes.
                        BEGIN_SEQUENCE_NUMBER, FixFieldsTypes.END_SEQUENCE_NUMBER, FixFieldsTypes.NEW_SEQUENCE_NUMBER, FixFieldsTypes.REFERENCED_SEQUENCE_NUMBER, FixFieldsTypes.ENCRYPT_METHOD, FixFieldsTypes.HEARTBEAT_INTERVAL,
                FixFieldsTypes.
                    GAP_FILL_FLAG, FixFieldsTypes.RESET_SEQ_NUMBER_FLAG, FixFieldsTypes.TEXT, FixFieldsTypes.TEST_REQ_ID, FixFieldsTypes.REFERENCED_TAG_ID, FixFieldsTypes.SESSION_REJECT_REASON, FixFieldsTypes.DEFAULT_APP_VERSION, FixFieldsTypes.CHECK_SUM]
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
        return null;
    }
}