package io.github.zlooo.fixyou.fix.commons

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.model.FixSpec

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
    char[][] getMessageTypes() {
        return [FixConstants.LOGON, ['D'] as char[]]
    }

    @Override
    ApplicationVersionID applicationVersionId() {
        return ApplicationVersionID.FIX50SP2
    }

    @Override
    int[] getRepeatingGroupFieldNumbers(int groupNumber) {
        return [] as int[]
    }
}
