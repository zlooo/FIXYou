package pl.zlooo.fixyou.netty.handler

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleStateEvent
import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import pl.zlooo.fixyou.netty.handler.admin.TestSpec
import pl.zlooo.fixyou.parser.model.FixMessage
import pl.zlooo.fixyou.session.SessionConfig
import pl.zlooo.fixyou.session.SessionID
import spock.lang.Specification

class MutableIdleStateHandlerTest extends Specification {

    private SessionConfig sessionConfig = new SessionConfig()
    private SessionID sessionID = new SessionID([] as char[], [] as char[], [] as char[])
    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(sessionConfig, sessionID, fixMessageObjectPool, TestSpec.INSTANCE)
    private MutableIdleStateHandler idleStateHandler = new MutableIdleStateHandler(sessionState)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private ChannelFuture channelFuture = Mock()

    def "should send heartbeat when write timeout occurs"() {
        setup:
        FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)

        when:
        idleStateHandler.channelIdle(channelHandlerContext, IdleStateEvent.WRITER_IDLE_STATE_EVENT)

        then:
        1 * fixMessageObjectPool.getAndRetain() >> fixMessage
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.HEARTBEAT
        !fixMessage.getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).isValueSet()
        0 * _
    }

    def "should send test request when first read timeout occurs"() {
        setup:
        FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)

        when:
        idleStateHandler.channelIdle(channelHandlerContext, IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT)

        then:
        1 * fixMessageObjectPool.getAndRetain() >> fixMessage
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.TEST_REQUEST
        fixMessage.getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).value == 'test'.toCharArray()
        0 * _
    }

    def "should close connection when second read timeout occurs"() {
        setup:
        FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)

        when:
        idleStateHandler.channelIdle(channelHandlerContext, IdleStateEvent.READER_IDLE_STATE_EVENT)

        then:
        1 * channelHandlerContext.close()
        0 * _
    }

    def "should do nothing when all timeout request occurs"() {
        when:
        idleStateHandler.channelIdle(channelHandlerContext, IdleStateEvent.ALL_IDLE_STATE_EVENT)

        then:
        0 * _
    }
}
