package io.github.zlooo.fixyou.netty.handler.validation


import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import spock.lang.Specification

import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class SessionAwareValidators_ValidatorsTest extends Specification {

    private static OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC)
    private static io.github.zlooo.fixyou.session.SessionID sessionID = new io.github.zlooo.fixyou.session.SessionID('beginString'.toCharArray(), 'senderCompId'.toCharArray(), 'targetCompId'.toCharArray())
    private io.github.zlooo.fixyou.parser.model.FixMessage fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(io.github.zlooo.fixyou.netty.handler.admin.TestSpec.INSTANCE)
    private io.github.zlooo.fixyou.session.SessionConfig sessionConfig = new io.github.zlooo.fixyou.session.SessionConfig()
    private DefaultObjectPool<io.github.zlooo.fixyou.parser.model.FixMessage> fixMessageObjectPool = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(sessionConfig, sessionID, fixMessageObjectPool, io.github.zlooo.fixyou.netty.handler.admin.TestSpec.INSTANCE)

    def "should validate session id"() {
        setup:
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = senderCompID
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = targetCompID

        when:
        def result = SessionAwareValidators.SESSION_ID_VALIDATOR.validator.apply(fixMessage, sessionState)

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
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.BEGIN_STRING_FIELD_NUMBER).value = beginString

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
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = sessionID.senderCompID
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = sessionID.targetCompID
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.BODY_LENGTH_FIELD_NUMBER).value = bodyLength

        when:
        def result = SessionAwareValidators.BODY_LENGTH_VALIDATOR.validator.apply(fixMessage, sessionState)

        then:
        (result == null) == expectedResult

        where:
        bodyLength                                                                    | expectedResult
        3 + sessionID.senderCompID.length + 1 + 3 + sessionID.targetCompID.length + 1 | true
        1                                                                             | false
    }

    def "should validate message type"() {
        setup:
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = messageType

        when:
        def result = SessionAwareValidators.MESSAGE_TYPE_VALIDATOR.validator.apply(fixMessage, sessionState)

        then:
        (result == null) == expectedResult

        where:
        messageType     | expectedResult
        ['D'] as char[] | true
        ['Z'] as char[] | false
    }

    def "should validate sending time"() {
        setup:
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SENDING_TIME_FIELD_NUMBER).value = sendingTime.format(io.github.zlooo.fixyou.FixConstants.UTC_TIMESTAMP_FORMATTER).toCharArray()
        def validator = SessionAwareValidators.createSendingTimeValidator(clock)

        when:
        def result = validator.validator.apply(fixMessage, sessionState)

        then:
        (result == null) == expectedResult

        where:

        sendingTime                                                                  | clock                                        | expectedResult
        now.minus(io.github.zlooo.fixyou.FixConstants.SENDING_TIME_ACCURACY_MILLIS + 10, ChronoUnit.MILLIS) | Clock.fixed(now.toInstant(), ZoneOffset.UTC) | false
        now.minus(io.github.zlooo.fixyou.FixConstants.SENDING_TIME_ACCURACY_MILLIS - 10, ChronoUnit.MILLIS) | Clock.fixed(now.toInstant(), ZoneOffset.UTC) | true
    }
}
