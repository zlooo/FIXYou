package io.github.zlooo.fixyou.netty.handler

import com.lmax.disruptor.dsl.Disruptor
import io.github.zlooo.fixyou.FIXYouConfiguration
import io.github.zlooo.fixyou.commons.memory.Region
import io.github.zlooo.fixyou.commons.memory.RegionPool
import io.github.zlooo.fixyou.commons.pool.ObjectPool
import io.github.zlooo.fixyou.fix.commons.FixMessageListener
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.AbstractNettyAwareFixMessageListener
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.parser.model.OffHeapFixMessage
import io.github.zlooo.fixyou.session.SessionID
import io.github.zlooo.fixyou.utils.UnsafeAccessor
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class FixMessageListenerInvokingHandlerTest extends Specification {

    private FixMessageListener fixMessageListener = Mock()
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionStateAttribute = Mock()
    private Attribute<Integer> ordinalNumberAttribute = Mock()
    private NettyHandlerAwareSessionState sessionState = Mock()
    private SessionID sessionID = new SessionID("", "", "")
    @Shared
    private final RegionPool regionPool = new RegionPool(1, 16 as short)
    @AutoCleanup
    private FixMessage fixMessage = new OffHeapFixMessage(regionPool)
    def fixYouConfiguration = FIXYouConfiguration.builder().separateIoFromAppThread(false).build()

    def setup() {
        fixMessage.retain() //fix message passed to this handler will have ref count == 1
    }

    def cleanupSpec() {
        regionPool.close()
    }

    def "should invoke fix message listener directly"() {
        setup:
        FixMessageListenerInvokingHandler handler = new FixMessageListenerInvokingHandler(fixMessageListener, fixYouConfiguration, Mock(ObjectPool))

        when:
        handler.channelRead(channelHandlerContext, fixMessage)

        then:
        handler.disruptor == null
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        1 * sessionState.getSessionId() >> sessionID
        1 * fixMessageListener.onFixMessage(sessionID, fixMessage)
        fixMessage.refCnt() == 1 //1 because autorelease is set to false
        0 * _
    }

    def "should invoke fix message listener via disruptor"() {
        setup:
        def fixMessageListener = new TestFixMessageListener()
        def region = new Region(UnsafeAccessor.UNSAFE.allocateMemory(256), 256 as short)
        def regionPool = Mock(ObjectPool, {
            4 * tryGetAndRetain() >> region
        })
        FixMessageListenerInvokingHandler handler = new FixMessageListenerInvokingHandler(fixMessageListener,
                                                                                          FIXYouConfiguration.builder().separateIoFromAppThread(true).fixMessageListenerInvokerDisruptorSize(4).build(),
                                                                                          regionPool)
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
        fixMessage.refCnt() == 1 //1 because autorelease is set to false
        0 * _

        cleanup:
        handler?.close()
        UnsafeAccessor.UNSAFE.freeMemory(region?.getStartingAddress())
    }

    def "should set channel is listener is instance of AbstractNettyAwareFixMessageListener"() {
        setup:
        TestFixMessageListener fixMessageListener = new TestFixMessageListener()
        FixMessageListenerInvokingHandler handler = new FixMessageListenerInvokingHandler(fixMessageListener, fixYouConfiguration, Mock(ObjectPool))

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
        } catch (IllegalStateException e) {
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
