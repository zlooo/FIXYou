package io.github.zlooo.fixyou.netty.handler.validation

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.SimpleFixMessage
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
        def fixMessage = new SimpleFixMessage()
        if (origSendingTime != null) {
            fixMessage.setTimestampValue(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER, origSendingTime.toInstant(ZoneOffset.UTC).toEpochMilli())
        }
        if (sendingTime != null) {
            fixMessage.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, sendingTime.toInstant(ZoneOffset.UTC).toEpochMilli())
        }
        return fixMessage
    }

    private static FixMessage logon(int fieldToReset = -1) {
        FixMessage logon = new SimpleFixMessage()
        logon.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, "beginString")
        logon.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, "senderCompID")
        logon.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, "targetCompID")
        logon.setLongValue(FixConstants.BODY_LENGTH_FIELD_NUMBER, 666L)
        logon.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 666L)
        logon.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.LOGON)
        logon.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, Instant.now().toEpochMilli())
        logon.setLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, 0L)
        logon.setLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, 15L)
        logon.setCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, ApplicationVersionID.FIX50SP2.value)
        if (fieldToReset > 0) {
            logon.removeField(fieldToReset)
        }
        return logon
    }

    private static FixMessage logonWithResetSeqNum(boolean setSequenceToOne) {
        def logonMessage = logon()
        logonMessage.setBooleanValue(FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER, true)
        if (setSequenceToOne) {
            logonMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 1L)
        }
        return logonMessage
    }
}
