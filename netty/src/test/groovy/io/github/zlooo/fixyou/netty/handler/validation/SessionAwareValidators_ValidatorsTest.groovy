package io.github.zlooo.fixyou.netty.handler.validation

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class SessionAwareValidators_ValidatorsTest extends Specification {

    private static OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC)
    private static SessionID sessionID = new SessionID('beginString', 'senderCompId', 'targetCompId')
    @AutoCleanup
    private FixMessage fixMessage = new NotPoolableFixMessage()
    private SessionConfig sessionConfig = new SessionConfig()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(sessionConfig, sessionID, TestSpec.INSTANCE)

    def "should validate session id"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, senderCompID)
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, targetCompID)

        when:
        def result = SessionAwareValidators.COMP_ID_VALIDATOR.validator.apply(fixMessage, sessionState)

        then:
        (result == null) == expectedResult

        where:
        senderCompID           | targetCompID           | expectedResult
        sessionID.senderCompID | sessionID.targetCompID | true
        'wrongSendercompId'    | sessionID.targetCompID | false
        sessionID.senderCompID | 'wrongTargetCompId'    | false
    }

    def "should validate begin string"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, beginString)

        when:
        def result = SessionAwareValidators.BEGIN_STRING_VALIDATOR.validator.apply(fixMessage, sessionState)

        then:
        (result == null) == expectedResult

        where:
        beginString           | expectedResult
        sessionID.beginString | true
        'wrongBeginString'    | false
    }

    def "should validate body length"() {
        setup:
        //just for reference
        //"8=FIXT.1.1\u00019=$bodyLength\u000149=senderCompId\u000156=targetCompId\u000110=000\u0001"
        fixMessage.setLongValue(FixConstants.BODY_LENGTH_FIELD_NUMBER, tagBodyLength)
        fixMessage.setBodyLength(realBodyLength)

        when:
        def result = SessionAwareValidators.BODY_LENGTH_VALIDATOR.validator.apply(fixMessage, sessionState)

        then:
        (result == null) == expectedResult

        where:
        tagBodyLength                                                                     | realBodyLength                                                                    | expectedResult
        3 + sessionID.senderCompID.length() + 1 + 3 + sessionID.targetCompID.length() + 1 | tagBodyLength                                                                     | true
        1                                                                                 | 3 + sessionID.senderCompID.length() + 1 + 3 + sessionID.targetCompID.length() + 1 | false
    }

    def "should validate message type"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, messageType)

        when:
        def result = SessionAwareValidators.MESSAGE_TYPE_VALIDATOR.validator.apply(fixMessage, sessionState)

        then:
        (result == null) == expectedResult

        where:
        messageType | expectedResult
        'D'         | true
        'Z'         | false
    }

    def "should validate sending time"() {
        setup:
        fixMessage.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, sendingTime.toInstant().toEpochMilli())
        def validator = SessionAwareValidators.createSendingTimeValidator(clock)

        when:
        def result = validator.validator.apply(fixMessage, sessionState)

        then:
        (result == null) == expectedResult

        where:

        sendingTime                                                                  | clock                                        | expectedResult
        now.minus(FixConstants.SENDING_TIME_ACCURACY_MILLIS + 10, ChronoUnit.MILLIS) | Clock.fixed(now.toInstant(), ZoneOffset.UTC) | false
        now.minus(FixConstants.SENDING_TIME_ACCURACY_MILLIS - 10, ChronoUnit.MILLIS) | Clock.fixed(now.toInstant(), ZoneOffset.UTC) | true
    }
}
