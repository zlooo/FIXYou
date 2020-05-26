package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.fix.commons.FixMessageListener
import io.github.zlooo.fixyou.netty.AbstractNettyAwareFixMessageListener
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.session.SessionID
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import spock.lang.Specification

class FixMessageListenerInvokingHandlerTest extends Specification {

    private FixMessageListener fixMessageListener = Mock()
    private FixMessageListenerInvokingHandler handler = new FixMessageListenerInvokingHandler(fixMessageListener)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionStateAttribute = Mock()
    private NettyHandlerAwareSessionState sessionState = Mock()
    private SessionID sessionID = new SessionID([] as char[], [] as char[], [] as char[])
    private io.github.zlooo.fixyou.parser.model.FixMessage fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(TestSpec.INSTANCE)

    def "should invoke fix message listener"() {
        when:
        handler.channelRead0(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        1 * sessionState.getSessionId() >> sessionID
        1 * fixMessageListener.onFixMessage(sessionID, fixMessage)
        0 * _
    }

    def "should set channel is listener is instance of AbstractNettyAwareFixMessageListener"() {
        setup:
        TestFixMessageListener fixMessageListener = new TestFixMessageListener()
        handler = new FixMessageListenerInvokingHandler(fixMessageListener)

        when:
        handler.channelActive(channelHandlerContext)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channelHandlerContext.fireChannelActive()
        fixMessageListener.channel == channel
    }

    private static class TestFixMessageListener extends AbstractNettyAwareFixMessageListener {

        @Override
        void onFixMessage(SessionID sessionID, io.github.zlooo.fixyou.parser.model.FixMessage fixMessage) {

        }
    }
}