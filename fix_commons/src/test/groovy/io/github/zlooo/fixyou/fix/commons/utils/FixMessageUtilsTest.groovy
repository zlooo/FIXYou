package io.github.zlooo.fixyou.fix.commons.utils

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.fix.commons.FixSpec50SP2
import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.model.FixSpec
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage
import io.github.zlooo.fixyou.session.SessionID
import spock.lang.AutoCleanup
import spock.lang.Specification

class FixMessageUtilsTest extends Specification {

    private static final FixSpec FIX_SPEC = new FixSpec50SP2()

    @AutoCleanup
    private NotPoolableFixMessage fixMessage = createFixMessage()

    def "should convert to reject message"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666)

        then:
        result.getLongValue(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER) == 123
        result.getLongValue(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER) == 666
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.REJECT
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to reject message with referenced tag number"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666, 777)

        then:
        result.getLongValue(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER) == 123
        result.getLongValue(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER) == 666
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.REJECT
        result.getLongValue(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER) == 777
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER)
    }

    def "should convert to reject message with referenced tag number and description"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666, 777, "testDescription".toCharArray())

        then:
        result.getLongValue(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER) == 123
        result.getLongValue(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER) == 666
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.REJECT
        result.getLongValue(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER) == 777
        result.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == "testDescription"
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER,
                                         FixConstants.TEXT_FIELD_NUMBER)
    }

    def "should convert to reject message with description"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666, "testDescription".toCharArray())

        then:
        result.getLongValue(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER) == 123
        result.getLongValue(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER) == 666
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.REJECT
        result.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == "testDescription"
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.TEXT_FIELD_NUMBER)
    }

    def "should convert to resend request"() {
        when:
        def result = FixMessageUtils.toResendRequest(fixMessage, 666, 667)

        then:
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.RESEND_REQUEST
        result.getLongValue(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER) == 666
        result.getLongValue(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER) == 667
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to logout message"() {
        when:
        def result = FixMessageUtils.toLogoutMessage(fixMessage, "logoutText".toCharArray())

        then:
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGOUT
        result.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == "logoutText"
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.TEXT_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to logon message with just default application version id set"() {
        setup:
        fixMessage.setLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, 1L)
        fixMessage.setLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, 2L)

        when:
        def result = FixMessageUtils.toLogonMessage(fixMessage, ApplicationVersionID.FIX50SP2.value)

        then:
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGON
        result.getLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER) == 1
        result.getLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER) == 2
        result.getCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).chars == ApplicationVersionID.FIX50SP2.value
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to logon message"() {
        when:
        def result = FixMessageUtils.toLogonMessage(fixMessage, ApplicationVersionID.FIX50SP2.value, 1, 2, true)

        then:
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGON
        result.getLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER) == 1
        result.getLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER) == 2
        result.getCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).chars == ApplicationVersionID.FIX50SP2.value
        result.getBooleanValue(FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER)
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER,
                                         FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to logon message with custom session id"() {
        setup:
        SessionID sessionID = new SessionID("beginString", "senderCompId", "targetCompId")

        when:
        def result = FixMessageUtils.toLogonMessage(fixMessage, ApplicationVersionID.FIX50SP2.value, 1, 2, true, sessionID, flip)

        then:
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGON
        result.getLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER) == 1
        result.getLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER) == 2
        result.getCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).chars == ApplicationVersionID.FIX50SP2.value
        result.getBooleanValue(FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER)
        result.getCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).toString() == expectedSenderCompId
        result.getCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).toString() == expectedTargetCompId
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
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.SEQUENCE_RESET
        result.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 1
        result.getLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER) == 2
        result.getBooleanValue(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER)
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.GAP_FILL_FLAG_FIELD_NUMBER,
                                         FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to heartbeat message"() {
        when:
        def result = FixMessageUtils.toHeartbeatMessage(fixMessage)

        then:
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.HEARTBEAT
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to heartbeat message with test request id"() {
        when:
        def result = FixMessageUtils.toHeartbeatMessage(fixMessage, "testRequestId")

        then:
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.HEARTBEAT
        result.getCharSequenceValue(FixConstants.TEST_REQ_ID_FIELD_NUMBER).toString() == "testRequestId"
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.TEST_REQ_ID_FIELD_NUMBER)
    }

    def "should convert to test request"() {
        when:
        def result = FixMessageUtils.toTestRequest(fixMessage, "testRequestId".toCharArray())

        then:
        result.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.TEST_REQUEST
        result.getCharSequenceValue(FixConstants.TEST_REQ_ID_FIELD_NUMBER).toString() == "testRequestId"
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.TEST_REQ_ID_FIELD_NUMBER)
    }

    def "should check if message is sequence reset"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, messageType)

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
            fixMessage.setBooleanValue(fieldNumber, valueToSet)
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
            fixMessage.setBooleanValue(fieldNumber, valueToSet)
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
        NotPoolableFixMessage fixMessage = new NotPoolableFixMessage()
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, msgType)

        expect:
        FixMessageUtils.isAdminMessage(fixMessage) == expectedResult

        cleanup:
        fixMessage?.close()

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

    NotPoolableFixMessage createFixMessage() {
        NotPoolableFixMessage message = new NotPoolableFixMessage()
        message.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, "FIXT1.1")
        message.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, "senderCompId")
        message.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, "targetCompId")
        message.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 123L)
        return message
    }

    void allFieldsDoNotHaveValueSetExcept(NotPoolableFixMessage message, int ... fieldNumbersWithValue) {
        def valueSetFields = fieldNumbersWithValue.toSet()
        def fieldsToCheck = FIX_SPEC.headerFieldsOrder.toList()
        FIX_SPEC.bodyFieldsOrder.each {fieldsToCheck::add}
        for (int fieldNumber : fieldsToCheck) {
            if (valueSetFields.contains(fieldNumber)) {
                assert message.isValueSet(fieldNumber)
            } else {
                assert !message.isValueSet(fieldNumber)
            }
        }
    }
}
