package io.github.zlooo.fixyou.netty.handler.validation

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.fix.commons.LogoutTexts
import io.github.zlooo.fixyou.fix.commons.RejectReasons
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.netty.utils.FixChannelListeners
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

class SessionAwareValidators_ValidationFailureActionsTest extends Specification {

    private static SessionID sessionID = new SessionID('beginString'.toCharArray(), 11, 'senderCompId'.toCharArray(), 12, 'targetCompId'.toCharArray(), 12)
    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)
    private SessionConfig sessionConfig = new SessionConfig()
    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(sessionConfig, sessionID, fixMessageObjectPool, TestSpec.INSTANCE)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private ChannelFuture channelFuture1 = Mock()
    private ChannelFuture channelFuture2 = Mock()

    def "should send reject and logout when original sending time is greater than sending time"() {
        setup:
        LocalDateTime now = LocalDateTime.now()
        fixMessage.getField(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER).value = now.plusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getField(FixConstants.SENDING_TIME_FIELD_NUMBER).value = now.minusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli()
        FixMessage logoutMessage = new FixMessage(TestSpec.INSTANCE)

        when:
        SessionAwareValidators.ORIG_SENDING_TIME_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.write(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value.toString() == String.valueOf(FixConstants.REJECT)
        fixMessage.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == RejectReasons.SENDING_TIME_ACCURACY_PROBLEM
        1 * fixMessageObjectPool.getAndRetain() >> logoutMessage
        1 * channelHandlerContext.writeAndFlush(logoutMessage) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture2
        1 * channelFuture2.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.CLOSE)
        logoutMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value.toString() == String.valueOf(FixConstants.LOGOUT)
        logoutMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value.toString() == String.valueOf(LogoutTexts.INACCURATE_SENDING_TIME)
        0 * _
    }

    def "should send reject and logout when sender or target comp id is wrong"() {
        setup:
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).value = sessionID.beginString
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = senderCompId
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = targetCompId
        FixMessage logout = new FixMessage(TestSpec.INSTANCE)

        when:
        SessionAwareValidators.COMP_ID_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.write(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value.toString() == String.valueOf(FixConstants.REJECT)
        fixMessage.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == RejectReasons.COMP_ID_PROBLEM
        1 * fixMessageObjectPool.getAndRetain() >> logout
        1 * channelHandlerContext.writeAndFlush(logout) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture2
        1 * channelFuture2.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.CLOSE)
        logout.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value.toString() == String.valueOf(FixConstants.LOGOUT)
        logout.getField(FixConstants.TEXT_FIELD_NUMBER).value.toString() == String.valueOf(LogoutTexts.INCORRECT_SENDER_OR_TARGET_COMP_ID)
        0 * _

        where:
        senderCompId                      | targetCompId
        'wrongSenderCompId'.toCharArray() | sessionID.targetCompID
        sessionID.senderCompID            | 'wrongTargetCompId'.toCharArray()
    }

    def "should send logout when begin string is wrong"() {
        setup:
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).value = 'wrongBeginString'.toCharArray()
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = sessionID.senderCompID
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = sessionID.targetCompID

        when:
        SessionAwareValidators.BEGIN_STRING_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture1
        1 * channelFuture1.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.CLOSE)
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value.toString() == String.valueOf(FixConstants.LOGOUT)
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value.toString() == String.valueOf(LogoutTexts.INCORRECT_BEGIN_STRING)
        0 * _
    }

    def "should do nothing if body length is incorrect"() {
        setup:
        def fixMessageAsString = "8=FIXT.1.1\u00019=1\u000149=senderCompId\u000156=targetCompId\u000110=000\u0001"
        fixMessage.setMessageByteSource(Unpooled.wrappedBuffer(fixMessageAsString.getBytes(StandardCharsets.US_ASCII)))
        fixMessage.getField(FixConstants.BODY_LENGTH_FIELD_NUMBER).setIndexes(13, 14)

        expect:
        SessionAwareValidators.BODY_LENGTH_VALIDATOR.validator.apply(fixMessage, sessionState) == ValidationFailureActions.RELEASE_MESSAGE
    }

    def "should send reject if message type is invalid"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = ['Z'] as char[]

        when:
        SessionAwareValidators.MESSAGE_TYPE_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value.toString() == String.valueOf(FixConstants.REJECT)
        fixMessage.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == RejectReasons.INVALID_MESSAGE_TYPE
        fixMessage.getField(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER).value == FixConstants.MESSAGE_TYPE_FIELD_NUMBER
        0 * _
    }

    def "should send reject and logout when sending time inaccuracy problem is detected"() {
        setup:
        LocalDateTime now = LocalDateTime.now()
        fixMessage.getField(FixConstants.SENDING_TIME_FIELD_NUMBER).value = now.minusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli()
        FixMessage logoutMessage = new FixMessage(TestSpec.INSTANCE)

        when:
        SessionAwareValidators.createSendingTimeValidator(Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)).validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.write(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value.toString() == String.valueOf(FixConstants.REJECT)
        fixMessage.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == RejectReasons.SENDING_TIME_ACCURACY_PROBLEM
        1 * fixMessageObjectPool.getAndRetain() >> logoutMessage
        1 * channelHandlerContext.writeAndFlush(logoutMessage) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture2
        1 * channelFuture2.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.CLOSE)
        logoutMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value.toString() == String.valueOf(FixConstants.LOGOUT)
        logoutMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value.toString() == String.valueOf(LogoutTexts.INACCURATE_SENDING_TIME)
        0 * _
    }
}
