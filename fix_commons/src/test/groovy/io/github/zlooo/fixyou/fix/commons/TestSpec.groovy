package io.github.zlooo.fixyou.fix.commons

class TestSpec implements io.github.zlooo.fixyou.model.FixSpec {

    static INSTANCE = new TestSpec()

    @Override
    int[] getFieldsOrder() {
        return [io.github.zlooo.fixyou.FixConstants.BEGIN_STRING_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants.BODY_LENGTH_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER, io.
                github.
                zlooo.
                fixyou.
                FixConstants
                                                                                                                                                                                                              .MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, io.
                github.
                zlooo.
                fixyou.
                FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants
                                                                     .SENDER_COMP_ID_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants
                                                                                                                                                                      .BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, io.
                github.
                zlooo.
                fixyou.
                FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants
                                                                 .NEW_SEQUENCE_NUMBER_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, io.
                github.
                zlooo.
                fixyou.
                FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants
                                                            .HEARTBEAT_INTERVAL_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants.GAP_FILL_FLAG_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants
                                                                                                                                                                .TEXT_FIELD_NUMBER, io.
                github.
                zlooo.
                fixyou.
                FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants
                                                                                                                                       .DEFAULT_APP_VERSION_ID_FIELD_NUMBER, io.github.zlooo.fixyou.FixConstants.CHECK_SUM_FIELD_NUMBER]
    }

    @Override
    io.github.zlooo.fixyou.model.FieldType[] getTypes() {
        return [io.github.zlooo.fixyou.parser.FixFieldsTypes.BEGIN_STRING, io.github.zlooo.fixyou.parser.FixFieldsTypes.BODY_LENGTH, io.github.zlooo.fixyou.parser.FixFieldsTypes.MESSAGE_TYPE, io.
                github.
                zlooo.
                fixyou.
                parser.
                FixFieldsTypes.MESSAGE_SEQUENCE_NUMBER, io.github.zlooo.fixyou.parser.FixFieldsTypes.POSS_DUP_FLAG,
                io.github.zlooo.fixyou.parser.FixFieldsTypes.SENDER_COMP_ID, io.github.zlooo.fixyou.parser.FixFieldsTypes.TARGET_COMP_ID,
                io.github.zlooo.fixyou.parser.FixFieldsTypes.BEGIN_SEQUENCE_NUMBER, io.github.zlooo.fixyou.parser.FixFieldsTypes
                                                                                      .END_SEQUENCE_NUMBER, io.github.zlooo.fixyou.parser.FixFieldsTypes.NEW_SEQUENCE_NUMBER, io.
                github.
                zlooo.
                fixyou.
                parser.
                FixFieldsTypes.REFERENCED_SEQUENCE_NUMBER, io.github.zlooo.fixyou.parser.FixFieldsTypes.ENCRYPT_METHOD, io.github.zlooo.fixyou.parser.FixFieldsTypes
                                                                                                                          .HEARTBEAT_INTERVAL, io.github.zlooo.fixyou.parser.FixFieldsTypes.GAP_FILL_FLAG, io.
                github.
                zlooo.
                fixyou.
                parser.
                FixFieldsTypes.TEXT, io.github.zlooo.fixyou.parser.FixFieldsTypes.REFERENCED_TAG_ID, io.github.zlooo.fixyou.parser.FixFieldsTypes.SESSION_REJECT_REASON,
                io.github.zlooo.fixyou.parser.FixFieldsTypes.DEFAULT_APP_VERSION, io.github.zlooo.fixyou.parser.FixFieldsTypes.CHECK_SUM]
    }

    @Override
    char[][] getMessageTypes() {
        return [io.github.zlooo.fixyou.FixConstants.LOGON, ['D'] as char[]]
    }

    @Override
    int highestFieldNumber() {
        final int[] fieldsOrder = getFieldsOrder()
        Arrays.sort(fieldsOrder)
        return fieldsOrder[fieldsOrder.length - 1]
    }

    @Override
    io.github.zlooo.fixyou.model.ApplicationVersionID applicationVersionId() {
        return io.github.zlooo.fixyou.model.ApplicationVersionID.FIX50SP2
    }

    @Override
    FieldNumberTypePair[] getChildPairSpec(int groupNumber) {
        return null;
    }
}
