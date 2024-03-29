package io.github.zlooo.fixyou.netty.utils

import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.Handlers
import io.github.zlooo.fixyou.netty.handler.MutableIdleStateHandler
import io.github.zlooo.fixyou.netty.handler.NettyResettablesNames
import io.github.zlooo.fixyou.netty.handler.SessionAwareChannelInboundHandler
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

    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(SessionConfig.builder().validationConfig(ValidationConfig.builder().validate(true).build()).build(), new SessionID("", "", ""),
                                                                                           TestSpec.INSTANCE)
    private ChannelHandler messageDecoder = Mock()
    private ChannelHandler genericDecoder = Mock()
    private ChannelHandler messageEncoder = Mock()
    private ChannelHandler adminMessageHandler = Mock()
    private ChannelHandler fixMessageListenerInvokingHandler = Mock()
    private ChannelHandler genericHandler = Mock()
    private ChannelHandler preValidator = Mock()
    private ChannelHandler postValidator = Mock()
    private SessionAwareChannelInboundHandler sessionHandler = Mock()
    private DelegatingChannelHandlerContext nmfCtx = Mock()
    private MutableIdleStateHandler idleStateHandler = Mock()
    private ChannelHandler flushConsolidationHandler = Mock()
    private Attribute sessionAttribute = Mock()

    void setup() {
        sessionState.resettables.putAll([(NettyResettablesNames.MESSAGE_DECODER)                                             : messageDecoder,
                                         (NettyResettablesNames.SESSION)                                                     : sessionHandler,
                                         (NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT): nmfCtx,
                                         (NettyResettablesNames.IDLE_STATE_HANDLER)                                          : idleStateHandler,
                                         (NettyResettablesNames.FLUSH_CONSOLIDATION_HANDLER)                                 : flushConsolidationHandler])
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
                containsExactly(Handlers.FLUSH_CONSOLIDATION_HANDLER.getName(), Handlers.MESSAGE_DECODER.getName(), Handlers.MESSAGE_ENCODER.getName(),
                                Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), Handlers.GENERIC.getName(),
                                Handlers.SESSION.getName(), Handlers.IDLE_STATE_HANDLER.getName(), Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName(), Handlers.ADMIN_MESSAGES.getName(), Handlers.LISTENER_INVOKER.getName())
    }

    def "should not add flush consolidation handler if config option is set to false"() {
        setup:
        NioSocketChannel channel = Mock()
        TestPipeline channelPipeline = pipeline()
        sessionState = new NettyHandlerAwareSessionState(SessionConfig.builder().validationConfig(ValidationConfig.builder().validate(true).build()).consolidateFlushes(false).build(), new SessionID("", "", ""),
                                                         TestSpec.INSTANCE)
        setup()

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
                containsExactly(Handlers.MESSAGE_DECODER.getName(), Handlers.MESSAGE_ENCODER.getName(), Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), Handlers.GENERIC.getName(),
                                Handlers.SESSION.getName(), Handlers.IDLE_STATE_HANDLER.getName(), Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName(), Handlers.ADMIN_MESSAGES.getName(), Handlers.LISTENER_INVOKER.getName())
    }

    def "should add flush consolidation and async executing handlers is config option is set to true"() {
        setup:
        NioSocketChannel channel = Mock()
        TestPipeline channelPipeline = pipeline()
        sessionState = new NettyHandlerAwareSessionState(SessionConfig.builder().validationConfig(ValidationConfig.builder().validate(true).build()).consolidateFlushes(true).build(), new SessionID("", "", ""),
                                                         TestSpec.INSTANCE)
        setup()

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
                containsExactly(Handlers.FLUSH_CONSOLIDATION_HANDLER.getName(), Handlers.MESSAGE_DECODER.getName(), Handlers.MESSAGE_ENCODER.getName(),
                                Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), Handlers.GENERIC.getName(),
                                Handlers.SESSION.getName(), Handlers.IDLE_STATE_HANDLER.getName(), Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName(), Handlers.ADMIN_MESSAGES.getName(), Handlers.LISTENER_INVOKER.getName())
    }

    def "should not add handlers which are on exclude list"() {
        setup:
        NioSocketChannel channel = Mock()
        ChannelPipeline channelPipeline = new TestPipeline()
        /**
         * should be same as in {@link io.github.zlooo.fixyou.netty.handler.FIXYouChannelInitializer#initChannel}
         */
        channelPipeline.addLast(Handlers.GENERIC_DECODER.getName(), genericDecoder)
                       .addLast(Handlers.MESSAGE_ENCODER.getName(), messageEncoder)
                       .addLast(Handlers.GENERIC.getName(), genericHandler)
                       .addLast(Handlers.ADMIN_MESSAGES.getName(), adminMessageHandler)
                       .addLast(Handlers.LISTENER_INVOKER.getName(), fixMessageListenerInvokingHandler)

        when:
        def result = PipelineUtils.addRequiredHandlersToPipeline(channel, sessionState, preValidator, postValidator, 30, Handlers.AFTER_SESSION_MESSAGE_VALIDATOR)

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
                containsExactly(Handlers.FLUSH_CONSOLIDATION_HANDLER.getName(), Handlers.MESSAGE_DECODER.getName(), Handlers.MESSAGE_ENCODER.getName(),
                                Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), Handlers.GENERIC.getName(),
                                Handlers.SESSION.getName(), Handlers.IDLE_STATE_HANDLER.getName(), Handlers.ADMIN_MESSAGES.getName(), Handlers.LISTENER_INVOKER.getName())
    }

    def "should not not add handler if handler is already added"() {
        setup:
        NioSocketChannel channel = Mock()
        ChannelPipeline channelPipeline = new TestPipeline()
        channelPipeline.addLast(Handlers.GENERIC_DECODER.getName(), genericDecoder)
                       .addLast(Handlers.MESSAGE_ENCODER.getName(), messageEncoder)
                       .addLast(Handlers.GENERIC.getName(), genericHandler)
                       .addLast(Handlers.SESSION.getName(), sessionHandler)
                       .addLast(Handlers.ADMIN_MESSAGES.getName(), adminMessageHandler)
                       .addLast(Handlers.LISTENER_INVOKER.getName(), fixMessageListenerInvokingHandler)

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
                containsExactly(Handlers.FLUSH_CONSOLIDATION_HANDLER.getName(), Handlers.MESSAGE_DECODER.getName(), Handlers.MESSAGE_ENCODER.getName(),
                                Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), Handlers.GENERIC.getName(),
                                Handlers.SESSION.getName(), Handlers.IDLE_STATE_HANDLER.getName(), Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName(), Handlers.ADMIN_MESSAGES.getName(), Handlers.LISTENER_INVOKER.getName())
    }

    def "should not add handler if dependant handler is not present"() {
        setup:
        NioSocketChannel channel = Mock()
        ChannelPipeline channelPipeline = new TestPipeline()
        channelPipeline.addLast(Handlers.MESSAGE_ENCODER.getName(), messageEncoder)
                       .addLast(Handlers.GENERIC.getName(), genericHandler)
                       .addLast(Handlers.ADMIN_MESSAGES.getName(), adminMessageHandler)
                       .addLast(Handlers.LISTENER_INVOKER.getName(), fixMessageListenerInvokingHandler)

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
                containsExactly(Handlers.FLUSH_CONSOLIDATION_HANDLER.getName(), Handlers.MESSAGE_ENCODER.getName(), Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), Handlers.GENERIC.getName(),
                                Handlers.SESSION.getName(),
                                Handlers.IDLE_STATE_HANDLER.getName(), Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName(), Handlers.ADMIN_MESSAGES.getName(), Handlers.LISTENER_INVOKER.getName())
    }

    private TestPipeline pipeline() {
        ChannelPipeline channelPipeline = new TestPipeline()
        /**
         * should be same as in {@link io.github.zlooo.fixyou.netty.handler.FIXYouChannelInitializer#initChannel}
         */
        channelPipeline.addLast(Handlers.GENERIC_DECODER.getName(), genericDecoder)
                       .addLast(Handlers.MESSAGE_ENCODER.getName(), messageEncoder)
                       .addLast(Handlers.GENERIC.getName(), genericHandler)
                       .addLast(Handlers.ADMIN_MESSAGES.getName(), adminMessageHandler)
                       .addLast(Handlers.LISTENER_INVOKER.getName(), fixMessageListenerInvokingHandler)
        return channelPipeline
    }
}
