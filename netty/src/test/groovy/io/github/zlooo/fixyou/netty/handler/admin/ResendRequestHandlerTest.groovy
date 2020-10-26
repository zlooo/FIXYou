package io.github.zlooo.fixyou.netty.handler.admin

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.MessageStore
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import spock.lang.Specification

class ResendRequestHandlerTest extends Specification {

    private DefaultObjectPool<RetransmitionSubscriber> fixMessageSubscriberPool = Mock()
    private ResendRequestHandler resendRequestHandler = new ResendRequestHandler(fixMessageSubscriberPool)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private FixMessage fixMessage = new FixMessage(new FieldCodec())
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionStateAttribute = Mock()
    private DefaultObjectPool<FixMessage> fixMessageObjectReadPool = Mock()
    private DefaultObjectPool<FixMessage> fixMessageObjectWritePool = Mock()
    private SessionID sessionID = new SessionID([] as char[], 0, [] as char[], 0, [] as char[], 0)
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig(), sessionID, fixMessageObjectReadPool, fixMessageObjectWritePool, TestSpec.INSTANCE)

    def "should start message retrieval and send when persistence is on"() {
        setup:
        sessionState.getSessionConfig().setPersistent(true)
        fixMessage.getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 666L
        fixMessage.getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 777L
        MessageStore messageStore = Mock()
        sessionState.getSessionConfig().setMessageStore(messageStore)
        RetransmitionSubscriber fixMessageSubscriber = new RetransmitionSubscriber()

        when:
        resendRequestHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 0
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        1 * fixMessageSubscriberPool.getAndRetain() >> fixMessageSubscriber
        1 * messageStore.getMessages(sessionID, 666L, 777L, fixMessageSubscriber)
        fixMessageSubscriber.fixMessagePool == fixMessageObjectWritePool
        fixMessageSubscriber.channelHandlerContext == channelHandlerContext
        0 * _
    }

    def "should send sequence reset gap fill message when persistence is not on"() {
        setup:
        sessionState.getSessionConfig().setPersistent(false)
        fixMessage.getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 666L
        fixMessage.getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 777L
        ChannelFuture channelFuture = Mock()

        when:
        resendRequestHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        fixMessage.refCnt() == 1
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.toString() == String.valueOf(FixConstants.SEQUENCE_RESET)
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue == 666L
        fixMessage.getField(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).longValue == 778L
        fixMessage.getField(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER).booleanValue
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        0 * _
    }
}
