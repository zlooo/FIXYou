package io.github.zlooo.fixyou.netty.handler

import com.lmax.disruptor.dsl.Disruptor
import io.github.zlooo.fixyou.FIXYouConfiguration
import io.github.zlooo.fixyou.fix.commons.FixMessageListener
import io.github.zlooo.fixyou.netty.AbstractNettyAwareFixMessageListener
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.SessionID
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class FixMessageListenerInvokingHandlerTest extends Specification {

    private FixMessageListener fixMessageListener = Mock()
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionStateAttribute = Mock()
    private Attribute<Integer> ordinalNumberAttribute = Mock()
    private NettyHandlerAwareSessionState sessionState = Mock()
    private SessionID sessionID = new SessionID([] as char[], 0, [] as char[], 0, [] as char[], 0)
    private FixMessage fixMessage = new FixMessage(new FieldCodec())
    def fixYouConfiguration = new FIXYouConfiguration.FIXYouConfigurationBuilder().separateIoFromAppThread(false).build()

    def "should invoke fix message listener directly"() {
        setup:
        FixMessageListenerInvokingHandler handler = new FixMessageListenerInvokingHandler(fixMessageListener, fixYouConfiguration, new FieldCodec())

        when:
        handler.channelRead(channelHandlerContext, fixMessage)

        then:
        handler.disruptor == null
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        1 * sessionState.getSessionId() >> sessionID
        1 * fixMessageListener.onFixMessage(sessionID, fixMessage)
        fixMessage.refCnt() == 0
        0 * _
    }

    def "should invoke fix message listener via disruptor"() {
        setup:
        def fixMessageListener = new TestFixMessageListener()
        FixMessageListenerInvokingHandler handler = new FixMessageListenerInvokingHandler(fixMessageListener,
                                                                                          new FIXYouConfiguration.FIXYouConfigurationBuilder().separateIoFromAppThread(true).fixMessageListenerInvokerDisruptorSize(4).build(),
                                                                                          new FieldCodec())
        PollingConditions pollingConditions = new PollingConditions()

        when:
        handler.channelRead(channelHandlerContext, fixMessage)
        pollingConditions.eventually {
            fixMessageListener.onFixMessageCalled
        }

        then:
        assertDisruptor(handler.disruptor)
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * channel.attr(FIXYouChannelInitializer.ORDINAL_NUMBER_KEY) >> ordinalNumberAttribute
        1 * ordinalNumberAttribute.get() >> 0
        1 * sessionStateAttribute.get() >> sessionState
        1 * sessionState.getSessionId() >> sessionID
        fixMessageListener.onFixMessageCalled
        fixMessage.refCnt() == 0
        0 * _

        cleanup:
        handler.close()
    }

    def "should set channel is listener is instance of AbstractNettyAwareFixMessageListener"() {
        setup:
        TestFixMessageListener fixMessageListener = new TestFixMessageListener()
        FixMessageListenerInvokingHandler handler = new FixMessageListenerInvokingHandler(fixMessageListener, fixYouConfiguration, new FieldCodec())

        when:
        handler.channelActive(channelHandlerContext)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channelHandlerContext.fireChannelActive()
        fixMessageListener.channel == channel
    }

    void assertDisruptor(Disruptor<FixMessageListenerInvokingHandler.Event> disruptor) {
        assert disruptor != null
        try {
            disruptor.start()
            assert false: "Disruptor has not been started"
        } catch (IllegalStateException) {
        }
    }

    private static class TestFixMessageListener extends AbstractNettyAwareFixMessageListener {

        private boolean onFixMessageCalled

        @Override
        void onFixMessage(SessionID sessionID, FixMessage fixMessage) {
            onFixMessageCalled = true
        }
    }
}
