package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.SimpleFixMessage
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleStateEvent
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class MutableIdleStateHandlerTest extends Specification {

    private SessionConfig sessionConfig = SessionConfig.builder().build()
    private SessionID sessionID = new SessionID("", "", "")
    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(sessionConfig, sessionID, TestSpec.INSTANCE)
    private MutableIdleStateHandler idleStateHandler = new MutableIdleStateHandler(sessionState, fixMessageObjectPool, 1, 2, 3)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private ChannelFuture channelFuture = Mock()

    def "should get reader idle time millis"() {
        expect:
        idleStateHandler.readerIdleTimeInMillis == TimeUnit.SECONDS.toMillis(1)
    }

    def "should get writer idle time millis"() {
        expect:
        idleStateHandler.writerIdleTimeInMillis == TimeUnit.SECONDS.toMillis(2)
    }

    def "should get all idle time millis"() {
        expect:
        idleStateHandler.allIdleTimeInMillis == TimeUnit.SECONDS.toMillis(3)
    }

    def "should get session state"() {
        expect:
        idleStateHandler.sessionState == sessionState
    }

    def "should send heartbeat when write timeout occurs"() {
        setup:
        FixMessage fixMessage = new SimpleFixMessage()

        when:
        idleStateHandler.channelIdle(channelHandlerContext, IdleStateEvent.WRITER_IDLE_STATE_EVENT)

        then:
        1 * fixMessageObjectPool.getAndRetain() >> fixMessage
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.HEARTBEAT)
        !fixMessage.isValueSet(FixConstants.TEST_REQ_ID_FIELD_NUMBER)
        0 * _

        cleanup:
        fixMessage?.close()
    }

    def "should send test request when first read timeout occurs"() {
        setup:
        FixMessage fixMessage = new SimpleFixMessage()

        when:
        idleStateHandler.channelIdle(channelHandlerContext, IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT)

        then:
        1 * fixMessageObjectPool.getAndRetain() >> fixMessage
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.TEST_REQUEST)
        fixMessage.getCharSequenceValue(FixConstants.TEST_REQ_ID_FIELD_NUMBER).toString() == 'test'
        0 * _

        cleanup:
        fixMessage?.close()
    }

    def "should close connection when second read timeout occurs"() {
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
