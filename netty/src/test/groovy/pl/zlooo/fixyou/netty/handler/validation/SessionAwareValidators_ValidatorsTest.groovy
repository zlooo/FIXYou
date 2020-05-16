package pl.zlooo.fixyou.netty.handler.validation

import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import pl.zlooo.fixyou.netty.handler.admin.TestSpec
import pl.zlooo.fixyou.parser.model.FixMessage
import pl.zlooo.fixyou.session.SessionConfig
import pl.zlooo.fixyou.session.SessionID
import spock.lang.Specification

import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class SessionAwareValidators_ValidatorsTest extends Specification {

    private static OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC)
    private static SessionID sessionID = new SessionID('beginString'.toCharArray(), 'senderCompId'.toCharArray(), 'targetCompId'.toCharArray())
    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)
    private SessionConfig sessionConfig = new SessionConfig()
    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(sessionConfig, sessionID, fixMessageObjectPool, TestSpec.INSTANCE)

    def "should validate session id"() {
        setup:
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = senderCompID
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = targetCompID

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
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).value = beginString

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
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = sessionID.senderCompID
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = sessionID.targetCompID
        fixMessage.getField(FixConstants.BODY_LENGTH_FIELD_NUMBER).value = bodyLength

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
        fixMessage.getField(FixConstants.SENDING_TIME_FIELD_NUMBER).value = sendingTime.format(FixConstants.UTC_TIMESTAMP_FORMATTER).toCharArray()
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
