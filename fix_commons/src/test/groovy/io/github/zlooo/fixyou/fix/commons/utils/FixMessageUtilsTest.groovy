package io.github.zlooo.fixyou.fix.commons.utils

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.parser.model.Field
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.SessionID
import spock.lang.Specification

class FixMessageUtilsTest extends Specification {

    private FixMessage fixMessage = createFixMessage()

    def "should convert to reject message"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666)

        then:
        result.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER).longValue == 123
        result.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).longValue == 666
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.REJECT
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to reject message with referenced tag number"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666, 777)

        then:
        result.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER).longValue == 123
        result.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).longValue == 666
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.REJECT
        result.getField(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER).longValue == 777
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER)
    }

    def "should convert to reject message with referenced tag number and description"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666, 777, "testDescription".toCharArray())

        then:
        result.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER).longValue == 123
        result.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).longValue == 666
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.REJECT
        result.getField(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER).longValue == 777
        result.getField(FixConstants.TEXT_FIELD_NUMBER).charSequenceValue.toString() == "testDescription"
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER,
                                         FixConstants.TEXT_FIELD_NUMBER)
    }

    def "should convert to reject message with description"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666, "testDescription".toCharArray())

        then:
        result.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER).longValue == 123
        result.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).longValue == 666
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.REJECT
        result.getField(FixConstants.TEXT_FIELD_NUMBER).charSequenceValue.toString() == "testDescription"
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.TEXT_FIELD_NUMBER)
    }

    def "should convert to resend request"() {
        when:
        def result = FixMessageUtils.toResendRequest(fixMessage, 666, 667)

        then:
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.RESEND_REQUEST
        result.getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).longValue == 666
        result.getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).longValue == 667
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to logout message"() {
        when:
        def result = FixMessageUtils.toLogoutMessage(fixMessage, "logoutText".toCharArray())

        then:
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.LOGOUT
        result.getField(FixConstants.TEXT_FIELD_NUMBER).charSequenceValue.toString() == "logoutText"
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.TEXT_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to logon message with just default application version id set"() {
        setup:
        fixMessage.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue = 1L
        fixMessage.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue = 2L

        when:
        def result = FixMessageUtils.toLogonMessage(fixMessage, ApplicationVersionID.FIX50SP2.value)

        then:
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.LOGON
        result.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue == 1
        result.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue == 2
        result.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue.chars == ApplicationVersionID.FIX50SP2.value
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to logon message"() {
        when:
        def result = FixMessageUtils.toLogonMessage(fixMessage, ApplicationVersionID.FIX50SP2.value, 1, 2, true)

        then:
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.LOGON
        result.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue == 1
        result.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue == 2
        result.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue.chars == ApplicationVersionID.FIX50SP2.value
        result.getField(FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER).booleanValue
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER,
                                         FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to logon message with custom session id"() {
        setup:
        SessionID sessionID = new SessionID("beginString".toCharArray(), 11, "senderCompId".toCharArray(), 12, "targetCompId".toCharArray(), 12)

        when:
        def result = FixMessageUtils.toLogonMessage(fixMessage, ApplicationVersionID.FIX50SP2.value, 1, 2, true, sessionID, flip)

        then:
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.LOGON
        result.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue == 1
        result.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue == 2
        result.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue.chars == ApplicationVersionID.FIX50SP2.value
        result.getField(FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER).booleanValue
        result.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).charSequenceValue.toString() == expectedSenderCompId
        result.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).charSequenceValue.toString() == expectedTargetCompId
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER,
                                         FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.SENDER_COMP_ID_FIELD_NUMBER, FixConstants.TARGET_COMP_ID_FIELD_NUMBER)

        where:
        flip  | expectedSenderCompId | expectedTargetCompId
        false | "senderCompId"       | "targetCompId"
        true  | "targetCompId"       | "senderCompId"
    }

    def "should convert to sequence reset message"() {
        when:
        def result = FixMessageUtils.toSequenceReset(fixMessage, 1, 2, true)

        then:
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.SEQUENCE_RESET
        result.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue == 1
        result.getField(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).longValue == 2
        result.getField(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER).booleanValue
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.GAP_FILL_FLAG_FIELD_NUMBER,
                                         FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to heartbeat message"() {
        when:
        def result = FixMessageUtils.toHeartbeatMessage(fixMessage)

        then:
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.HEARTBEAT
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to heartbeat message with test request id"() {
        when:
        def result = FixMessageUtils.toHeartbeatMessage(fixMessage, "testRequestId".toCharArray(),13)

        then:
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.HEARTBEAT
        result.getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).charSequenceValue.toString() == "testRequestId"
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.TEST_REQ_ID_FIELD_NUMBER)
    }

    def "should convert to test request"() {
        when:
        def result = FixMessageUtils.toTestRequest(fixMessage, "testRequestId".toCharArray())

        then:
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.chars == FixConstants.TEST_REQUEST
        result.getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).charSequenceValue.toString() == "testRequestId"
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.TEST_REQ_ID_FIELD_NUMBER)
    }

    def "should check if message is sequence reset"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue = messageType

        expect:
        FixMessageUtils.isSequenceReset(fixMessage) == expected

        where:
        messageType                 | expected
        FixConstants.HEARTBEAT      | false
        FixConstants.TEST_REQUEST   | false
        FixConstants.RESEND_REQUEST | false
        FixConstants.REJECT         | false
        FixConstants.LOGOUT         | false
        FixConstants.LOGON          | false
        FixConstants.SEQUENCE_RESET | true
        "D".toCharArray()           | false
        "8".toCharArray()           | false
        "AJ".toCharArray()          | false
    }

    def "should check if boolean field is set"() {
        setup:
        if (setField) {
            fixMessage.getField(fieldNumber).booleanValue = valueToSet
        }

        expect:
        FixMessageUtils.hasBooleanFieldSet(fixMessage, fieldNumber) == expected

        where:
        setField | fieldNumber                                          | valueToSet | expected
        false    | FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER | true       | false
        false    | 1100                                                 | true       | false
        true     | FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER | true       | true
        true     | FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER | false      | false
    }

    def "should check if field is set"() {
        setup:
        if (setField) {
            fixMessage.getField(fieldNumber).booleanValue = valueToSet
        }

        expect:
        FixMessageUtils.hasField(fixMessage, fieldNumber) == expected

        where:
        setField | fieldNumber                                          | valueToSet | expected
        false    | FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER | true       | false
        false    | 1100                                                 | true       | false
        true     | FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER | true       | true
        true     | FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER | false      | true
    }

    def "should check if message is administrative"() {
        setup:
        FixMessage fixMessage = new FixMessage(new FieldCodec())
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue = msgType

        expect:
        FixMessageUtils.isAdminMessage(fixMessage) == expectedResult

        where:
        msgType                     | expectedResult
        FixConstants.HEARTBEAT      | true
        FixConstants.TEST_REQUEST   | true
        FixConstants.RESEND_REQUEST | true
        FixConstants.REJECT         | true
        FixConstants.LOGOUT         | true
        FixConstants.LOGON          | true
        FixConstants.SEQUENCE_RESET | true
        "D".toCharArray()           | false
        "8".toCharArray()           | false
        "AJ".toCharArray()          | false
    }

    FixMessage createFixMessage() {
        FixMessage message = new FixMessage(new FieldCodec())
        message.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).charSequenceValue = "FIXT1.1".toCharArray()
        message.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).charSequenceValue = "senderCompId".toCharArray()
        message.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).charSequenceValue = "targetCompId".toCharArray()
        message.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 123L
        return message
    }

    void allFieldsDoNotHaveValueSetExcept(FixMessage message, int ... fieldNumbersWithValue) {
        def valueSetFields = fieldNumbersWithValue.toSet()
        for (Field field : message.allFields) {
            if (field != null) {
                if (valueSetFields.contains(field.number)) {
                    assert field.isValueSet()
                } else {
                    assert !field.isValueSet()
                }
            }
        }
    }
}
