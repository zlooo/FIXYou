package io.github.zlooo.fixyou.netty.handler.validation

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import spock.lang.Specification

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class ValidationOperationsTest extends Specification {

    private static LocalDateTime NOW = LocalDateTime.now()

    def "should check sending time"() {
        expect:
        ValidationOperations.checkOrigSendingTime(fixMessage) == result

        where:
        fixMessage                                                                 | result
        createFixMessage(null, LocalDateTime.now())                                | true
        createFixMessage(LocalDateTime.now().minusSeconds(1), LocalDateTime.now()) | true
        createFixMessage(LocalDateTime.now().plusSeconds(1), LocalDateTime.now())  | false
        createFixMessage(LocalDateTime.now().minusMinutes(1), LocalDateTime.now()) | true
        createFixMessage(LocalDateTime.now().plusMinutes(1), LocalDateTime.now())  | false
        createFixMessage(LocalDateTime.now().minusHours(1), LocalDateTime.now())   | true
        createFixMessage(LocalDateTime.now().plusHours(1), LocalDateTime.now())    | false
        createFixMessage(NOW, NOW)                                                 | true
    }

    def "should validate logon message"() {
        expect:
        ValidationOperations.isValidLogonMessage(message) == result

        where:
        message                                                  | result
        logon()                                                  | true
        logon(FixConstants.BEGIN_STRING_FIELD_NUMBER)            | false
        logon(FixConstants.SENDER_COMP_ID_FIELD_NUMBER)          | false
        logon(FixConstants.TARGET_COMP_ID_FIELD_NUMBER)          | false
        logon(FixConstants.BODY_LENGTH_FIELD_NUMBER)             | false
        logon(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) | false
        logon(FixConstants.MESSAGE_TYPE_FIELD_NUMBER)            | false
        logon(FixConstants.SENDING_TIME_FIELD_NUMBER)            | false
        logon(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER)          | false
        logon(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER)      | false
        logon(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER)  | false
        logonWithResetSeqNum(true)                               | true
        logonWithResetSeqNum(false)                              | false
    }

    private static FixMessage createFixMessage(LocalDateTime origSendingTime, LocalDateTime sendingTime) {
        def fixMessage = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        if (origSendingTime != null) {
            fixMessage.getField(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER).setTimestampValue(origSendingTime.toInstant(ZoneOffset.UTC).toEpochMilli())
        }
        if (sendingTime != null) {
            fixMessage.getField(FixConstants.SENDING_TIME_FIELD_NUMBER).setTimestampValue(sendingTime.toInstant(ZoneOffset.UTC).toEpochMilli())
        }
        return fixMessage
    }

    private static FixMessage logon(int fieldToReset = -1) {
        FixMessage logon = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        logon.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).charSequenceValue = "beginString".toCharArray()
        logon.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).charSequenceValue = "senderCompID".toCharArray()
        logon.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).charSequenceValue = "targetCompID".toCharArray()
        logon.getField(FixConstants.BODY_LENGTH_FIELD_NUMBER).longValue = 666L
        logon.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 666L
        logon.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue = FixConstants.LOGON
        logon.getField(FixConstants.SENDING_TIME_FIELD_NUMBER).timestampValue = Instant.now().toEpochMilli()
        logon.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue = 0L
        logon.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue = 15L
        logon.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue = ApplicationVersionID.FIX50SP2.value
        if (fieldToReset > 0) {
            logon.getField(fieldToReset).reset()
        }
        return logon
    }

    private static FixMessage logonWithResetSeqNum(boolean setSequenceToOne) {
        def logonMessage = logon()
        logonMessage.getField(FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER).booleanValue = true
        if (setSequenceToOne) {
            logonMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 1L
        }
        return logonMessage
    }
}
