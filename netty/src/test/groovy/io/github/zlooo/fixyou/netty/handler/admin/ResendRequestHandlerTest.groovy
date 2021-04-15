package io.github.zlooo.fixyou.netty.handler.admin

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage
import io.github.zlooo.fixyou.session.MessageStore
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import spock.lang.AutoCleanup
import spock.lang.Specification

class ResendRequestHandlerTest extends Specification {

    private DefaultObjectPool<RetransmitionSubscriber> fixMessageSubscriberPool = Mock()
    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock()
    private ResendRequestHandler resendRequestHandler = new ResendRequestHandler(fixMessageSubscriberPool, fixMessageObjectPool)
    private ChannelHandlerContext channelHandlerContext = Mock()
    @AutoCleanup
    private FixMessage fixMessage = new NotPoolableFixMessage()
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionStateAttribute = Mock()
    private SessionID sessionID = new SessionID("", "", "")
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig(), sessionID, TestSpec.INSTANCE)

    def "should start message retrieval and send when persistence is on"() {
        setup:
        sessionState.getSessionConfig().setPersistent(true)
        fixMessage.setLongValue(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, 666L)
        fixMessage.setLongValue(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, 777L)
        MessageStore messageStore = Mock()
        sessionState.getSessionConfig().setMessageStore(messageStore)
        RetransmitionSubscriber fixMessageSubscriber = new RetransmitionSubscriber()

        when:
        resendRequestHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 1 //1 because handler does not release and NotPoolableFixMessage has refCnt 1 after creation
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        1 * fixMessageSubscriberPool.getAndRetain() >> fixMessageSubscriber
        1 * messageStore.getMessages(sessionID, 666L, 777L, fixMessageSubscriber)
        fixMessageSubscriber.fixMessagePool == fixMessageObjectPool
        fixMessageSubscriber.channelHandlerContext == channelHandlerContext
        0 * _
    }

    def "should send sequence reset gap fill message when persistence is not on"() {
        setup:
        sessionState.getSessionConfig().setPersistent(false)
        fixMessage.setLongValue(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, 666L)
        fixMessage.setLongValue(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, 777L)
        ChannelFuture channelFuture = Mock()

        when:
        resendRequestHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        fixMessage.refCnt() == 1 + 1 //+1 because fix message is being reused as sequence reset
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.SEQUENCE_RESET
        fixMessage.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 666L
        fixMessage.getLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER) == 778L
        fixMessage.getBooleanValue(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER)
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        0 * _
    }
}
