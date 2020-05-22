package io.github.zlooo.fixyou.netty.utils

import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.github.zlooo.fixyou.session.ValidationConfig
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.Attribute
import org.assertj.core.api.Assertions
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class PipelineUtilsTest extends Specification {

    private DefaultObjectPool<io.github.zlooo.fixyou.parser.model.FixMessage> fixMessageObjectPool = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig().setValidationConfig(new ValidationConfig().setValidate(true)), new SessionID([] as char[], [] as char[], [] as char[]),
                                                                                           fixMessageObjectPool, TestSpec.INSTANCE)
    private ChannelHandler messageEncoder = Mock()
    private ChannelHandler messageDecoder = Mock()
    private ChannelHandler genericDecoder = Mock()
    private ChannelHandler adminMessageHandler = Mock()
    private ChannelHandler fixMessageListenerInvokingHandler = Mock()
    private ChannelHandler genericHandler = Mock()
    private ChannelHandler preValidator = Mock()
    private ChannelHandler postValidator = Mock()
    private io.github.zlooo.fixyou.netty.handler.SessionAwareChannelInboundHandler sessionHandler = Mock()
    private DelegatingChannelHandlerContext nmfCtx = Mock()
    private io.github.zlooo.fixyou.netty.handler.MutableIdleStateHandler idleStateHandler = Mock()
    private ChannelHandler flushConsolidationHandler = Mock()
    private Attribute sessionAttribute = Mock()

    void setup() {
        sessionState.resettables.putAll([(io.github.zlooo.fixyou.netty.handler.NettyResettablesNames.MESSAGE_ENCODER)                                             : messageEncoder,
                                         (io.github.zlooo.fixyou.netty.handler.NettyResettablesNames.MESSAGE_DECODER)                                             : messageDecoder,
                                         (io.github.zlooo.fixyou.netty.handler.NettyResettablesNames.SESSION)                                                     : sessionHandler,
                                         (io.github.zlooo.fixyou.netty.handler.NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT): nmfCtx,
                                         (io.github.zlooo.fixyou.netty.handler.NettyResettablesNames.IDLE_STATE_HANDLER)                                          : idleStateHandler,
                                         (io.github.zlooo.fixyou.netty.handler.NettyResettablesNames.FLUSH_CONSOLIDATION_HANDLER)                                 : flushConsolidationHandler])
    }

    def "should add all required handlers"() {
        setup:
        NioSocketChannel channel = Mock()
        TestPipeline channelPipeline = pipeline()

        when:
        def result = PipelineUtils.addRequiredHandlersToPipeline(channel, sessionState, preValidator, postValidator, 30)

        then:
        result == sessionHandler
        1 * channel.pipeline() >> channelPipeline
        !sessionState.getResettables().isEmpty()
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.set(sessionState)
        1 * idleStateHandler.setReaderIdleTimeNanos(TimeUnit.SECONDS.toNanos(30) * PipelineUtils.TEST_REQUEST_MULTIPLIER)
        1 * idleStateHandler.setWriterIdleTimeNanos(TimeUnit.SECONDS.toNanos(30))
        0 * _
        Assertions.
                assertThat(channelPipeline.names()).
                containsExactly(io.github.zlooo.fixyou.netty.handler.Handlers.FLUSH_CONSOLIDATION_HANDLER.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.MESSAGE_DECODER.getName(), io.
                        github.
                        zlooo.
                        fixyou.
                        netty.
                        handler.
                        Handlers.MESSAGE_ENCODER.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC.getName(),
                                io.github.zlooo.fixyou.netty.handler.Handlers.SESSION.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.IDLE_STATE_HANDLER.getName(), io.
                        github.
                        zlooo.
                        fixyou.
                        netty.
                        handler.
                        Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.ADMIN_MESSAGES.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.LISTENER_INVOKER.getName())
    }

    def "should not add flush consolidation handler is config option is set to false"() {
        setup:
        NioSocketChannel channel = Mock()
        TestPipeline channelPipeline = pipeline()
        sessionState.getSessionConfig().setConsolidateFlushes(false)

        when:
        def result = PipelineUtils.addRequiredHandlersToPipeline(channel, sessionState, preValidator, postValidator, 30)

        then:
        result == sessionHandler
        1 * channel.pipeline() >> channelPipeline
        !sessionState.getResettables().isEmpty()
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.set(sessionState)
        1 * idleStateHandler.setReaderIdleTimeNanos(TimeUnit.SECONDS.toNanos(30) * PipelineUtils.TEST_REQUEST_MULTIPLIER)
        1 * idleStateHandler.setWriterIdleTimeNanos(TimeUnit.SECONDS.toNanos(30))
        0 * _
        Assertions.
                assertThat(channelPipeline.names()).
                containsExactly(io.github.zlooo.fixyou.netty.handler.Handlers.MESSAGE_DECODER.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.MESSAGE_ENCODER.getName(), io.
                        github.
                        zlooo.
                        fixyou.
                        netty.
                        handler.
                        Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC.getName(),
                                io.github.zlooo.fixyou.netty.handler.Handlers.SESSION.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.IDLE_STATE_HANDLER.getName(), io.
                        github.
                        zlooo.
                        fixyou.
                        netty.
                        handler.
                        Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.ADMIN_MESSAGES.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.LISTENER_INVOKER.getName())
    }

    def "should not add handlers which are on exclude list"() {
        setup:
        NioSocketChannel channel = Mock()
        ChannelPipeline channelPipeline = new TestPipeline()
        /**
         * should be same as in {@link io.github.zlooo.fixyou.netty.handler.FIXYouChannelInitializer#initChannel}
         */
        channelPipeline.addLast(io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC_DECODER.getName(), genericDecoder)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC.getName(), genericHandler)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.ADMIN_MESSAGES.getName(), adminMessageHandler)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.LISTENER_INVOKER.getName(), fixMessageListenerInvokingHandler)

        when:
        def result = PipelineUtils.addRequiredHandlersToPipeline(channel, sessionState, preValidator, postValidator, 30, io.github.zlooo.fixyou.netty.handler.Handlers.AFTER_SESSION_MESSAGE_VALIDATOR)

        then:
        result == sessionHandler
        1 * channel.pipeline() >> channelPipeline
        !sessionState.getResettables().isEmpty()
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.set(sessionState)
        1 * idleStateHandler.setReaderIdleTimeNanos(TimeUnit.SECONDS.toNanos(30) * PipelineUtils.TEST_REQUEST_MULTIPLIER)
        1 * idleStateHandler.setWriterIdleTimeNanos(TimeUnit.SECONDS.toNanos(30))
        0 * _
        Assertions.
                assertThat(channelPipeline.names()).
                containsExactly(io.github.zlooo.fixyou.netty.handler.Handlers.FLUSH_CONSOLIDATION_HANDLER.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.MESSAGE_DECODER.getName(), io.
                        github.
                        zlooo.
                        fixyou.
                        netty.
                        handler.
                        Handlers.MESSAGE_ENCODER.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC.getName(),
                                io.github.zlooo.fixyou.netty.handler.Handlers.SESSION.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.IDLE_STATE_HANDLER.getName(), io.
                        github.
                        zlooo.
                        fixyou.
                        netty.
                        handler.
                        Handlers.ADMIN_MESSAGES.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.LISTENER_INVOKER.getName())
    }

    def "should not not add handler if handler is already added"() {
        setup:
        NioSocketChannel channel = Mock()
        ChannelPipeline channelPipeline = new TestPipeline()
        channelPipeline.addLast(io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC_DECODER.getName(), genericDecoder)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC.getName(), genericHandler)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.SESSION.getName(), sessionHandler)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.ADMIN_MESSAGES.getName(), adminMessageHandler)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.LISTENER_INVOKER.getName(), fixMessageListenerInvokingHandler)

        when:
        def result = PipelineUtils.addRequiredHandlersToPipeline(channel, sessionState, preValidator, postValidator, 30)

        then:
        result == null
        1 * channel.pipeline() >> channelPipeline
        !sessionState.getResettables().isEmpty()
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.set(sessionState)
        1 * idleStateHandler.setReaderIdleTimeNanos(TimeUnit.SECONDS.toNanos(30) * PipelineUtils.TEST_REQUEST_MULTIPLIER)
        1 * idleStateHandler.setWriterIdleTimeNanos(TimeUnit.SECONDS.toNanos(30))
        0 * _
        Assertions.
                assertThat(channelPipeline.names()).
                containsExactly(io.github.zlooo.fixyou.netty.handler.Handlers.FLUSH_CONSOLIDATION_HANDLER.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.MESSAGE_DECODER.getName(), io.
                        github.
                        zlooo.
                        fixyou.
                        netty.
                        handler.
                        Handlers.MESSAGE_ENCODER.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC.getName(),
                                io.github.zlooo.fixyou.netty.handler.Handlers.SESSION.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.IDLE_STATE_HANDLER.getName(), io.
                        github.
                        zlooo.
                        fixyou.
                        netty.
                        handler.
                        Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.ADMIN_MESSAGES.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.LISTENER_INVOKER.getName())
    }

    def "should not add handler if dependant handler is not present"() {
        setup:
        NioSocketChannel channel = Mock()
        ChannelPipeline channelPipeline = new TestPipeline()
        channelPipeline.addLast(io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC.getName(), genericHandler)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.ADMIN_MESSAGES.getName(), adminMessageHandler)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.LISTENER_INVOKER.getName(), fixMessageListenerInvokingHandler)

        when:
        def result = PipelineUtils.addRequiredHandlersToPipeline(channel, sessionState, preValidator, postValidator, 30)

        then:
        result == sessionHandler
        1 * channel.pipeline() >> channelPipeline
        !sessionState.getResettables().isEmpty()
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.set(sessionState)
        1 * idleStateHandler.setReaderIdleTimeNanos(TimeUnit.SECONDS.toNanos(30) * PipelineUtils.TEST_REQUEST_MULTIPLIER)
        1 * idleStateHandler.setWriterIdleTimeNanos(TimeUnit.SECONDS.toNanos(30))
        0 * _
        Assertions.
                assertThat(channelPipeline.names()).
                containsExactly(io.github.zlooo.fixyou.netty.handler.Handlers.FLUSH_CONSOLIDATION_HANDLER.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.MESSAGE_ENCODER.getName(), io.
                        github.
                        zlooo.
                        fixyou.
                        netty.
                        handler.
                        Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.SESSION.getName(),
                                io.github.zlooo.fixyou.netty.handler.Handlers.IDLE_STATE_HANDLER.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName(), io.
                        github.
                        zlooo.
                        fixyou.
                        netty.
                        handler.
                        Handlers.ADMIN_MESSAGES.getName(), io.github.zlooo.fixyou.netty.handler.Handlers.LISTENER_INVOKER.getName())
    }

    private TestPipeline pipeline() {
        ChannelPipeline channelPipeline = new TestPipeline()
        /**
         * should be same as in {@link io.github.zlooo.fixyou.netty.handler.FIXYouChannelInitializer#initChannel}
         */
        channelPipeline.addLast(io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC_DECODER.getName(), genericDecoder)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.GENERIC.getName(), genericHandler)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.ADMIN_MESSAGES.getName(), adminMessageHandler)
                       .addLast(io.github.zlooo.fixyou.netty.handler.Handlers.LISTENER_INVOKER.getName(), fixMessageListenerInvokingHandler)
        return channelPipeline
    }
}
