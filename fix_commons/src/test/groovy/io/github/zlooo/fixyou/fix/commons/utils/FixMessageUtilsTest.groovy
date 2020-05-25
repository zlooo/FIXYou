package io.github.zlooo.fixyou.fix.commons.utils

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.fix.commons.TestSpec
import io.github.zlooo.fixyou.parser.model.AbstractField
import io.github.zlooo.fixyou.parser.model.FixMessage
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class FixMessageUtilsTest extends Specification {

    private FixMessage fixMessage = createFixMessage()

    def "should convert to reject message"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666)

        then:
        result.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER).getFieldData().toString(StandardCharsets.US_ASCII) == "123"
        result.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == 666
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.REJECT
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER)
    }

    def "should convert to reject message with referenced tag number"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666, 777)

        then:
        result.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER).getFieldData().toString(StandardCharsets.US_ASCII) == "123"
        result.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == 666
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.REJECT
        result.getField(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER).value == 777
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER)
    }

    def "should convert to reject message with referenced tag number and description"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666, 777, "testDescription".toCharArray())

        then:
        result.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER).getFieldData().toString(StandardCharsets.US_ASCII) == "123"
        result.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == 666
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.REJECT
        result.getField(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER).value == 777
        result.getField(FixConstants.TEXT_FIELD_NUMBER).value == "testDescription".toCharArray()
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER,
                                         FixConstants.TEXT_FIELD_NUMBER)
    }

    def "should convert to reject message with description"() {
        when:
        def result = FixMessageUtils.toRejectMessage(fixMessage, 666, "testDescription".toCharArray())

        then:
        result.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER).getFieldData().toString(StandardCharsets.US_ASCII) == "123"
        result.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == 666
        result.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.REJECT
        result.getField(FixConstants.TEXT_FIELD_NUMBER).value == "testDescription".toCharArray()
        allFieldsDoNotHaveValueSetExcept(fixMessage, FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.TEXT_FIELD_NUMBER)
    }

    def "should check if message is administrative"() {
        setup:
        FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = msgType

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
        FixMessage message = new FixMessage(TestSpec.INSTANCE)
        message.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).value = "FIXT1.1".toCharArray()
        message.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 123L
        return message
    }

    void allFieldsDoNotHaveValueSetExcept(FixMessage message, int ... fieldNumbersWithValue) {
        def valueSetFields = fieldNumbersWithValue.toSet()
        for (AbstractField field : message.fields) {
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
