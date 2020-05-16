package pl.zlooo.fixyou.netty.handler.admin


import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import pl.zlooo.fixyou.parser.model.FixMessage
import pl.zlooo.fixyou.session.MessageStore
import pl.zlooo.fixyou.session.SessionConfig
import pl.zlooo.fixyou.session.SessionID
import spock.lang.Specification

class ResendRequestHandlerTest extends Specification {

    private DefaultObjectPool<RetransmitionSubscriber> fixMessageSubscriberPool = Mock()
    private ResendRequestHandler resendRequestHandler = new ResendRequestHandler(fixMessageSubscriberPool)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionStateAttribute = Mock()
    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock()
    private SessionID sessionID = new SessionID([] as char[], [] as char[], [] as char[])
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig(), sessionID, fixMessageObjectPool, TestSpec.INSTANCE)

    def "should start message retrieval and send when persistence is on"() {
        setup:
        sessionState.getSessionConfig().setPersistent(true)
        fixMessage.getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).value = 666L
        fixMessage.getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).value = 777L
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
        fixMessageSubscriber.fixMessagePool == fixMessageObjectPool
        fixMessageSubscriber.channelHandlerContext == channelHandlerContext
        0 * _
    }

    def "should send sequence reset gap fill message when persistence is not on"() {
        setup:
        sessionState.getSessionConfig().setPersistent(false)
        fixMessage.getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).value = 666L
        fixMessage.getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).value = 777L
        ChannelFuture channelFuture = Mock()

        when:
        resendRequestHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        fixMessage.refCnt() == 1
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.SEQUENCE_RESET
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value == 666L
        fixMessage.getField(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).value == 778L
        fixMessage.getField(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER).value == true
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        0 * _
    }
}
