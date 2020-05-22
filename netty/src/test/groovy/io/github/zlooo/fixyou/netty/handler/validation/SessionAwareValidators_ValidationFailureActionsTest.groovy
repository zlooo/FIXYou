package io.github.zlooo.fixyou.netty.handler.validation

import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.utils.FixChannelListeners
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import spock.lang.Specification

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

class SessionAwareValidators_ValidationFailureActionsTest extends Specification {

    private static io.github.zlooo.fixyou.session.SessionID sessionID = new io.github.zlooo.fixyou.session.SessionID('beginString'.toCharArray(), 'senderCompId'.toCharArray(), 'targetCompId'.toCharArray())
    private io.github.zlooo.fixyou.parser.model.FixMessage fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(io.github.zlooo.fixyou.netty.handler.admin.TestSpec.INSTANCE)
    private io.github.zlooo.fixyou.session.SessionConfig sessionConfig = new io.github.zlooo.fixyou.session.SessionConfig()
    private DefaultObjectPool<io.github.zlooo.fixyou.parser.model.FixMessage> fixMessageObjectPool = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(sessionConfig, sessionID, fixMessageObjectPool, io.github.zlooo.fixyou.netty.handler.admin.TestSpec.INSTANCE)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private ChannelFuture channelFuture1 = Mock()
    private ChannelFuture channelFuture2 = Mock()

    def "should send reject and logout when original sending time is greater than sending time"() {
        setup:
        LocalDateTime now = LocalDateTime.now()
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER).value = io.github.zlooo.fixyou.FixConstants.UTC_TIMESTAMP_FORMATTER.format(now.plusMinutes(1)).toCharArray()
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SENDING_TIME_FIELD_NUMBER).value = io.github.zlooo.fixyou.FixConstants.UTC_TIMESTAMP_FORMATTER.format(now.minusMinutes(1)).toCharArray()
        io.github.zlooo.fixyou.parser.model.FixMessage logoutMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(io.github.zlooo.fixyou.netty.handler.admin.TestSpec.INSTANCE)

        when:
        SessionAwareValidators.SENDING_TIME_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.write(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == io.github.zlooo.fixyou.FixConstants.REJECT
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == io.github.zlooo.fixyou.fix.commons.RejectReasons.SENDING_TIME_ACCURACY_PROBLEM
        1 * fixMessageObjectPool.getAndRetain() >> logoutMessage
        1 * channelHandlerContext.writeAndFlush(logoutMessage) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture2
        1 * channelFuture2.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.CLOSE)
        logoutMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == io.github.zlooo.fixyou.FixConstants.LOGOUT
        logoutMessage.getField(io.github.zlooo.fixyou.FixConstants.TEXT_FIELD_NUMBER).value == io.github.zlooo.fixyou.fix.commons.LogoutTexts.INACCURATE_SENDING_TIME
        0 * _
    }

    def "should send reject and logout when sender or target comp id is wrong"() {
        setup:
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.BEGIN_STRING_FIELD_NUMBER).value = sessionID.beginString
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = senderCompId
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = targetCompId
        io.github.zlooo.fixyou.parser.model.FixMessage logout = new io.github.zlooo.fixyou.parser.model.FixMessage(io.github.zlooo.fixyou.netty.handler.admin.TestSpec.INSTANCE)

        when:
        SessionAwareValidators.SESSION_ID_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.write(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == io.github.zlooo.fixyou.FixConstants.REJECT
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == io.github.zlooo.fixyou.fix.commons.RejectReasons.COMP_ID_PROBLEM
        1 * fixMessageObjectPool.getAndRetain() >> logout
        1 * channelHandlerContext.writeAndFlush(logout) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture2
        1*channelFuture2.addListener(FixChannelListeners.LOGOUT_SENT)>>channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.CLOSE)
        logout.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == io.github.zlooo.fixyou.FixConstants.LOGOUT
        logout.getField(io.github.zlooo.fixyou.FixConstants.TEXT_FIELD_NUMBER).value == io.github.zlooo.fixyou.fix.commons.LogoutTexts.INCORRECT_SENDER_OR_TARGET_COMP_ID
        0 * _

        where:
        senderCompId                      | targetCompId
        'wrongSenderCompId'.toCharArray() | sessionID.targetCompID
        sessionID.senderCompID            | 'wrongTargetCompId'.toCharArray()
    }

    def "should send logout when begin string is wrong"() {
        setup:
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.BEGIN_STRING_FIELD_NUMBER).value = 'wrongBeginString'.toCharArray()
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = sessionID.senderCompID
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = sessionID.targetCompID

        when:
        SessionAwareValidators.BEGIN_STRING_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture1
        1 * channelFuture1.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.CLOSE)
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == io.github.zlooo.fixyou.FixConstants.LOGOUT
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TEXT_FIELD_NUMBER).value == io.github.zlooo.fixyou.fix.commons.LogoutTexts.INCORRECT_BEGIN_STRING
        0 * _
    }

    def "should do nothing if body length is incorrect"() {
        setup:
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = sessionID.senderCompID
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = sessionID.targetCompID
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.BODY_LENGTH_FIELD_NUMBER).value = 1

        expect:
        SessionAwareValidators.BODY_LENGTH_VALIDATOR.validator.apply(fixMessage, sessionState) == ValidationFailureActions.RELEASE_MESSAGE
    }

    def "should send reject if message type is invalid"() {
        setup:
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = ['Z'] as char[]

        when:
        SessionAwareValidators.MESSAGE_TYPE_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == io.github.zlooo.fixyou.FixConstants.REJECT
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == io.github.zlooo.fixyou.fix.commons.RejectReasons.INVALID_MESSAGE_TYPE
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER).value == io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER
        0 * _
    }

    def "should send reject and logout when sending time inaccuracy problem is detected"() {
        setup:
        LocalDateTime now = LocalDateTime.now()
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SENDING_TIME_FIELD_NUMBER).value = io.github.zlooo.fixyou.FixConstants.UTC_TIMESTAMP_FORMATTER.format(now.minusMinutes(1)).toCharArray()
        io.github.zlooo.fixyou.parser.model.FixMessage logoutMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(io.github.zlooo.fixyou.netty.handler.admin.TestSpec.INSTANCE)

        when:
        SessionAwareValidators.createSendingTimeValidator(Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)).validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.write(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == io.github.zlooo.fixyou.FixConstants.REJECT
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == io.github.zlooo.fixyou.fix.commons.RejectReasons.SENDING_TIME_ACCURACY_PROBLEM
        1 * fixMessageObjectPool.getAndRetain() >> logoutMessage
        1 * channelHandlerContext.writeAndFlush(logoutMessage) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture2
        1 * channelFuture2.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.CLOSE)
        logoutMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == io.github.zlooo.fixyou.FixConstants.LOGOUT
        logoutMessage.getField(io.github.zlooo.fixyou.FixConstants.TEXT_FIELD_NUMBER).value == io.github.zlooo.fixyou.fix.commons.LogoutTexts.INACCURATE_SENDING_TIME
        0 * _
    }
}
