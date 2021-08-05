package io.github.zlooo.fixyou.netty.handler.validation

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.ObjectPool
import io.github.zlooo.fixyou.fix.commons.LogoutTexts
import io.github.zlooo.fixyou.fix.commons.RejectReasons
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.SimpleFixMessage
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.netty.utils.FixChannelListeners
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import spock.lang.Specification

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

class SessionAwareValidators_ValidationFailureActionsTest extends Specification {

    private static SessionID sessionID = new SessionID('beginString', 'senderCompId', 'targetCompId')
    private FixMessage fixMessage = new SimpleFixMessage()
    private SessionConfig sessionConfig = SessionConfig.builder().build()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(sessionConfig, sessionID, TestSpec.INSTANCE)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private ChannelFuture channelFuture1 = Mock()
    private ChannelFuture channelFuture2 = Mock()
    private ObjectPool<FixMessage> fixMessageObjectPool = Mock()

    def "should send reject and logout when original sending time is greater than sending time"() {
        setup:
        LocalDateTime now = LocalDateTime.now()
        fixMessage.setTimestampValue(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER, now.plusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli())
        fixMessage.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, now.minusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli())
        FixMessage logoutMessage = new SimpleFixMessage()

        when:
        SessionAwareValidators.ORIG_SENDING_TIME_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.write(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.REJECT)
        fixMessage.getLongValue(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER) == RejectReasons.SENDING_TIME_ACCURACY_PROBLEM
        1 * fixMessageObjectPool.getAndRetain() >> logoutMessage
        1 * channelHandlerContext.writeAndFlush(logoutMessage) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture2
        1 * channelFuture2.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.CLOSE)
        logoutMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.LOGOUT)
        logoutMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == String.valueOf(LogoutTexts.INACCURATE_SENDING_TIME)
        0 * _

        cleanup:
        logoutMessage?.close()
    }

    def "should send reject and logout when sender or target comp id is wrong"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, sessionID.beginString)
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, senderCompId)
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, targetCompId)
        FixMessage logout = new SimpleFixMessage()

        when:
        SessionAwareValidators.COMP_ID_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.write(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.REJECT)
        fixMessage.getLongValue(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER) == RejectReasons.COMP_ID_PROBLEM
        1 * fixMessageObjectPool.getAndRetain() >> logout
        1 * channelHandlerContext.writeAndFlush(logout) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture2
        1 * channelFuture2.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.CLOSE)
        logout.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.LOGOUT)
        logout.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == String.valueOf(LogoutTexts.INCORRECT_SENDER_OR_TARGET_COMP_ID)
        0 * _

        cleanup:
        logout?.close()

        where:
        senderCompId           | targetCompId
        'wrongSenderCompId'    | sessionID.targetCompID
        sessionID.senderCompID | 'wrongTargetCompId'
    }

    def "should send logout when begin string is wrong"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, 'wrongBeginString')
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, sessionID.senderCompID)
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, sessionID.targetCompID)

        when:
        SessionAwareValidators.BEGIN_STRING_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture1
        1 * channelFuture1.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.CLOSE)
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.LOGOUT)
        fixMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == String.valueOf(LogoutTexts.INCORRECT_BEGIN_STRING)
        0 * _
    }

    def "should do nothing if body length is incorrect"() {
        setup:
        //just for reference
        //"8=FIXT.1.1\u00019=1\u000149=senderCompId\u000156=targetCompId\u000110=000\u0001"
        fixMessage.setLongValue(FixConstants.BODY_LENGTH_FIELD_NUMBER, 1)
        fixMessage.setBodyLength(32)

        expect:
        SessionAwareValidators.BODY_LENGTH_VALIDATOR.validator.apply(fixMessage, sessionState) == ValidationFailureActions.DO_NOTHING
    }

    def "should send reject if message type is invalid"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, 'Z')

        when:
        SessionAwareValidators.MESSAGE_TYPE_VALIDATOR.validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.REJECT)
        fixMessage.getLongValue(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER) == RejectReasons.INVALID_MESSAGE_TYPE
        fixMessage.getLongValue(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER) == FixConstants.MESSAGE_TYPE_FIELD_NUMBER
        0 * _
    }

    def "should send reject and logout when sending time inaccuracy problem is detected"() {
        setup:
        LocalDateTime now = LocalDateTime.now()
        fixMessage.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, now.minusMinutes(1).toInstant(ZoneOffset.UTC).toEpochMilli())
        FixMessage logoutMessage = new SimpleFixMessage()

        when:
        SessionAwareValidators.createSendingTimeValidator(Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)).validator.apply(fixMessage, sessionState).perform(channelHandlerContext, fixMessage, fixMessageObjectPool)

        then:
        1 * channelHandlerContext.write(fixMessage) >> channelFuture1
        1 * channelFuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.REJECT)
        fixMessage.getLongValue(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER) == RejectReasons.SENDING_TIME_ACCURACY_PROBLEM
        1 * fixMessageObjectPool.getAndRetain() >> logoutMessage
        1 * channelHandlerContext.writeAndFlush(logoutMessage) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture2
        1 * channelFuture2.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.CLOSE)
        logoutMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.LOGOUT)
        logoutMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == String.valueOf(LogoutTexts.INACCURATE_SENDING_TIME)
        0 * _

        cleanup:
        logoutMessage?.close()
    }
}
