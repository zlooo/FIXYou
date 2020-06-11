package io.github.zlooo.fixyou.netty.handler.validation

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class SessionAwareValidators_ValidatorsTest extends Specification {

    private static OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC)
    private static SessionID sessionID = new SessionID('beginString'.toCharArray(), 11, 'senderCompId'.toCharArray(), 12, 'targetCompId'.toCharArray(), 12)
    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)
    private SessionConfig sessionConfig = new SessionConfig()
    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(sessionConfig, sessionID, fixMessageObjectPool, TestSpec.INSTANCE)

    def "should validate session id"() {
        setup:
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = senderCompID
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = targetCompID

        when:
        def result = SessionAwareValidators.COMP_ID_VALIDATOR.validator.apply(fixMessage, sessionState)

        then:
        (result == null) == expectedResult

        where:
        senderCompID                      | targetCompID                      | expectedResult
        sessionID.senderCompID            | sessionID.targetCompID            | true
        'wrongSendercompId'.toCharArray() | sessionID.targetCompID            | false
        sessionID.senderCompID            | 'wrongTargetCompId'.toCharArray() | false
    }

    def "should validate begin string"() {
        setup:
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).value = beginString

        when:
        def result = SessionAwareValidators.BEGIN_STRING_VALIDATOR.validator.apply(fixMessage, sessionState)

        then:
        (result == null) == expectedResult

        where:
        beginString                      | expectedResult
        sessionID.beginString            | true
        'wrongBeginString'.toCharArray() | false
    }

    def "should validate body length"() {
        setup:
        def fixMessageAsString = "8=FIXT.1.1\u00019=$bodyLength\u000149=senderCompId\u000156=targetCompId\u000110=000\u0001"
        fixMessage.setMessageByteSource(Unpooled.wrappedBuffer(fixMessageAsString.getBytes(StandardCharsets.US_ASCII)))
        fixMessage.getField(FixConstants.BODY_LENGTH_FIELD_NUMBER).setIndexes(13, 13 + bodyLength.toString().length())

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
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = messageType

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
        fixMessage.getField(FixConstants.SENDING_TIME_FIELD_NUMBER).value = sendingTime.toInstant().toEpochMilli()
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
