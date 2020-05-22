package io.github.zlooo.fixyou.netty.handler.admin

import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
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
    private io.github.zlooo.fixyou.parser.model.FixMessage fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(TestSpec.INSTANCE)
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionStateAttribute = Mock()
    private DefaultObjectPool<io.github.zlooo.fixyou.parser.model.FixMessage> fixMessageObjectPool = Mock()
    private io.github.zlooo.fixyou.session.SessionID sessionID = new io.github.zlooo.fixyou.session.SessionID([] as char[], [] as char[], [] as char[])
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new io.github.zlooo.fixyou.session.SessionConfig(), sessionID, fixMessageObjectPool, TestSpec.INSTANCE)

    def "should start message retrieval and send when persistence is on"() {
        setup:
        sessionState.getSessionConfig().setPersistent(true)
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).value = 666L
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).value = 777L
        io.github.zlooo.fixyou.session.MessageStore messageStore = Mock()
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
        fixMessageSubscriber.fixMessagePool == fixMessageObjectPool
        fixMessageSubscriber.channelHandlerContext == channelHandlerContext
        0 * _
    }

    def "should send sequence reset gap fill message when persistence is not on"() {
        setup:
        sessionState.getSessionConfig().setPersistent(false)
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).value = 666L
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).value = 777L
        ChannelFuture channelFuture = Mock()

        when:
        resendRequestHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        fixMessage.refCnt() == 1
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == io.github.zlooo.fixyou.FixConstants.SEQUENCE_RESET
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value == 666L
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).value == 778L
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.GAP_FILL_FLAG_FIELD_NUMBER).value == true
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        0 * _
    }
}
