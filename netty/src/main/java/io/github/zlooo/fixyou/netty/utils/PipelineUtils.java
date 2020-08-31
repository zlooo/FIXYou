package io.github.zlooo.fixyou.netty.utils;

import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.netty.handler.Handlers;
import io.github.zlooo.fixyou.netty.handler.MutableIdleStateHandler;
import io.github.zlooo.fixyou.netty.handler.NettyResettablesNames;
import io.github.zlooo.fixyou.netty.handler.SessionAwareChannelInboundHandler;
import io.github.zlooo.fixyou.session.ValidationConfig;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@UtilityClass
public class PipelineUtils {

    private static final double TEST_REQUEST_MULTIPLIER = 1.2; //"some reasonable period of time" which according to fix spec is 20% of heartbeat interval

    @Nullable
    public static SessionAwareChannelInboundHandler addRequiredHandlersToPipeline(Channel channel, NettyHandlerAwareSessionState sessionState, ChannelHandler preSessionValidator, ChannelHandler postSessionValidator,
                                                                                  long heartbeatIntervalSeconds, Handlers... excludes) {
        final ChannelPipeline pipeline = channel.pipeline();
        final Map<String, Resettable> resettables = sessionState.getResettables();
        if (canHandlerBeAdded(Handlers.MESSAGE_DECODER, Handlers.GENERIC_DECODER, pipeline, excludes)) {
            pipeline.replace(Handlers.GENERIC_DECODER.getName(), Handlers.MESSAGE_DECODER.getName(), (ChannelHandler) resettables.get(NettyResettablesNames.MESSAGE_DECODER));
        }
        final ValidationConfig validationConfig = addHandlersBasedOnGenericHandler(sessionState, preSessionValidator, pipeline, resettables, excludes);
        final SessionAwareChannelInboundHandler sessionHandler;
        if (canHandlerBeAdded(Handlers.SESSION, Handlers.ADMIN_MESSAGES, pipeline, excludes)) {
            sessionHandler = (SessionAwareChannelInboundHandler) resettables.get(NettyResettablesNames.SESSION);
            pipeline.addBefore(Handlers.ADMIN_MESSAGES.getName(), Handlers.SESSION.getName(), sessionHandler);
        } else {
            sessionHandler = null;
        }
        if (validationConfig.isValidate() && canHandlerBeAdded(Handlers.AFTER_SESSION_MESSAGE_VALIDATOR, Handlers.SESSION, pipeline, excludes)) {
            pipeline.addAfter(Handlers.SESSION.getName(), Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName(), postSessionValidator);
        }
        if (canHandlerBeAdded(Handlers.IDLE_STATE_HANDLER, Handlers.SESSION, pipeline, excludes)) {
            final MutableIdleStateHandler idleStateHandler = (MutableIdleStateHandler) resettables.get(NettyResettablesNames.IDLE_STATE_HANDLER);
            idleStateHandler.setReaderIdleTimeNanos(TimeUnit.SECONDS.toNanos((long) (heartbeatIntervalSeconds * TEST_REQUEST_MULTIPLIER)));
            idleStateHandler.setWriterIdleTimeNanos(TimeUnit.SECONDS.toNanos(heartbeatIntervalSeconds));
            pipeline.addAfter(Handlers.SESSION.getName(), Handlers.IDLE_STATE_HANDLER.getName(), idleStateHandler);
        }
        if (sessionState.getSessionConfig().isConsolidateFlushes() && canHandlerBeAdded(Handlers.FLUSH_CONSOLIDATION_HANDLER, Handlers.MESSAGE_ENCODER, pipeline, excludes)) {
            pipeline.addFirst(Handlers.FLUSH_CONSOLIDATION_HANDLER.getName(), (ChannelHandler) resettables.get(NettyResettablesNames.FLUSH_CONSOLIDATION_HANDLER));
        }
        channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY).set(sessionState);
        return sessionHandler;
    }

    private static ValidationConfig addHandlersBasedOnGenericHandler(NettyHandlerAwareSessionState sessionState, ChannelHandler preSessionValidator, ChannelPipeline pipeline, Map<String, Resettable> resettables, Handlers[] excludes) {
        if (canHandlerBeAdded(Handlers.MESSAGE_ENCODER, Handlers.GENERIC, pipeline, excludes)) {
            pipeline.addBefore(Handlers.GENERIC.getName(), Handlers.MESSAGE_ENCODER.getName(), (ChannelHandler) resettables.get(NettyResettablesNames.MESSAGE_ENCODER));
        }
        if (sessionState.getSessionConfig().isPersistent() && canHandlerBeAdded(Handlers.MESSAGE_STORE_HANDLER, Handlers.GENERIC, pipeline, excludes)) {
            pipeline.addAfter(Handlers.GENERIC.getName(), Handlers.MESSAGE_STORE_HANDLER.getName(), (ChannelHandler) resettables.get(NettyResettablesNames.MESSAGE_STORE_HANDLER));
        }
        final ValidationConfig validationConfig = sessionState.getSessionConfig().getValidationConfig();
        if (validationConfig.isValidate() && canHandlerBeAdded(Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR, Handlers.GENERIC, pipeline, excludes)) {
            pipeline.addBefore(Handlers.GENERIC.getName(), Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), preSessionValidator);
        }
        return validationConfig;
    }

    private static boolean canHandlerBeAdded(Handlers handlerToAdd, Handlers baseHandler, ChannelPipeline pipeline, Handlers[] excludes) {
        return !ArrayUtils.contains(excludes, handlerToAdd) && pipeline.get(baseHandler.getName()) != null && pipeline.get(handlerToAdd.getName()) == null;
    }
}
