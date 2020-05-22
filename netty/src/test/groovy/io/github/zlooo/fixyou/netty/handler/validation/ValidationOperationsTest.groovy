package io.github.zlooo.fixyou.netty.handler.validation


import spock.lang.Specification

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ValidationOperationsTest extends Specification {

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
    }

    def "should validate logon message"() {
        expect:
        ValidationOperations.isValidLogonMessage(message) == result

        where:
        message                                                  | result
        logon()                                                  | true
        logon(io.github.zlooo.fixyou.FixConstants.BEGIN_STRING_FIELD_NUMBER)            | false
        logon(io.github.zlooo.fixyou.FixConstants.SENDER_COMP_ID_FIELD_NUMBER)          | false
        logon(io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER)          | false
        logon(io.github.zlooo.fixyou.FixConstants.BODY_LENGTH_FIELD_NUMBER)             | false
        logon(io.github.zlooo.fixyou.FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) | false
        logon(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER)            | false
        logon(io.github.zlooo.fixyou.FixConstants.SENDING_TIME_FIELD_NUMBER)            | false
        logon(io.github.zlooo.fixyou.FixConstants.ENCRYPT_METHOD_FIELD_NUMBER)          | false
        logon(io.github.zlooo.fixyou.FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER)      | false
        logon(io.github.zlooo.fixyou.FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER)  | false
        logonWithResetSeqNum(true)                               | true
        logonWithResetSeqNum(false)                              | false
    }

    private static io.github.zlooo.fixyou.parser.model.FixMessage createFixMessage(LocalDateTime origSendingTime, LocalDateTime sendingTime) {
        def fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(io.github.zlooo.fixyou.netty.handler.admin.TestSpec.INSTANCE)
        if (origSendingTime != null) {
            fixMessage.<io.github.zlooo.fixyou.parser.model.CharArrayField> getField(io.github.zlooo.fixyou.FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER).setValue(io.
                    github.
                    zlooo.
                    fixyou.
                    FixConstants.UTC_TIMESTAMP_FORMATTER.format(origSendingTime).toCharArray())
        }
        if (sendingTime != null) {
            fixMessage.<io.github.zlooo.fixyou.parser.model.CharArrayField> getField(io.github.zlooo.fixyou.FixConstants.SENDING_TIME_FIELD_NUMBER).setValue(io.
                    github.
                    zlooo.
                    fixyou.
                    FixConstants.UTC_TIMESTAMP_FORMATTER.format(sendingTime).toCharArray())
        }
        return fixMessage
    }

    private static io.github.zlooo.fixyou.parser.model.FixMessage logon(int fieldToReset = -1) {
        io.github.zlooo.fixyou.parser.model.FixMessage logon = new io.github.zlooo.fixyou.parser.model.FixMessage(io.github.zlooo.fixyou.netty.handler.admin.TestSpec.INSTANCE)
        logon.getField(io.github.zlooo.fixyou.FixConstants.BEGIN_STRING_FIELD_NUMBER).value = "beginString".toCharArray()
        logon.getField(io.github.zlooo.fixyou.FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = "senderCompID".toCharArray()
        logon.getField(io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = "targetCompID".toCharArray()
        logon.getField(io.github.zlooo.fixyou.FixConstants.BODY_LENGTH_FIELD_NUMBER).value = 666L
        logon.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 666L
        logon.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = io.github.zlooo.fixyou.FixConstants.LOGON
        logon.getField(io.github.zlooo.fixyou.FixConstants.SENDING_TIME_FIELD_NUMBER).value = io.github.zlooo.fixyou.FixConstants.UTC_TIMESTAMP_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC)).toCharArray()
        logon.getField(io.github.zlooo.fixyou.FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).value = 0L
        logon.getField(io.github.zlooo.fixyou.FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).value = 15L
        logon.getField(io.github.zlooo.fixyou.FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).value = io.github.zlooo.fixyou.model.ApplicationVersionID.FIX50SP2.value
        if (fieldToReset > 0) {
            logon.getField(fieldToReset).reset()
        }
        return logon
    }

    private static io.github.zlooo.fixyou.parser.model.FixMessage logonWithResetSeqNum(boolean setSequenceToOne) {
        def logonMessage = logon()
        logonMessage.getField(io.github.zlooo.fixyou.FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER).value = true
        if (setSequenceToOne) {
            logonMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 1L
        }
        return logonMessage
    }
}
