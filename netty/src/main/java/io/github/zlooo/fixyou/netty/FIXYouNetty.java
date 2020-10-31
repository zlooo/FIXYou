package io.github.zlooo.fixyou.netty;

import io.github.zlooo.fixyou.Engine;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.FIXYouException;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.fix.commons.FixMessageListener;
import io.github.zlooo.fixyou.fix.commons.config.validator.ConfigValidator;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.netty.handler.DaggerFixYouNettyComponent;
import io.github.zlooo.fixyou.netty.handler.FixYouNettyComponent;
import io.github.zlooo.fixyou.netty.utils.FixChannelListeners;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.session.SessionID;
import io.github.zlooo.fixyou.session.SessionRegistry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@UtilityClass
public class FIXYouNetty {

    @Nonnull
    public static <T extends FixMessageListener> Engine create(@Nonnull FIXYouConfiguration fixYouConfiguration, @Nonnull T fixMessageListener) {
        final FixYouNettyComponent fixYouNettyComponent = DaggerFixYouNettyComponent.builder().fixMessageListener(fixMessageListener).fixYouConfiguration(fixYouConfiguration).build();
        final ConfigValidator configValidator =
                fixYouConfiguration.isInitiator() ? fixYouNettyComponent.initiatorConfigValidator() : fixYouNettyComponent.acceptorConfigValidator();
        final Set<String> errorMessages = configValidator.validateConfig(fixYouConfiguration);
        if (!errorMessages.isEmpty()) {
            throw new FIXYouException("Configuration is invalid: " + String.join(", ", errorMessages));
        }
        if (fixYouConfiguration.isInitiator()) {
            return new FIXYouNettyInitiator(fixYouNettyComponent, fixYouConfiguration, configValidator);
        } else {
            return new FIXYouNettyAcceptor(fixYouNettyComponent, fixYouConfiguration, configValidator);
        }
    }

    @Nonnull
    public static Future sendMessage(@Nonnull Consumer<FixMessage> fixMessageCreator, @Nonnull SessionID sessionID, @Nonnull Engine engine) { //TODO not sure about this API, Future<ChannelFuture> that does not look very well
        final SessionRegistry<NettyHandlerAwareSessionState> sessionRegistry = ((AbstractFIXYouNetty) engine).fixYouNettyComponent.sessionRegistry();
        final NettyHandlerAwareSessionState sessionState = sessionRegistry.getStateForSessionRequired(sessionID);
        final Channel channel = sessionState.getChannel();
        if (channel != null) {
            return channel.eventLoop().submit(() -> {
                final FixMessage fixMessage = (FixMessage) ((AbstractFIXYouNetty) engine).fixYouNettyComponent.fixMessageObjectPool().getAndRetain();
                fixMessageCreator.accept(fixMessage);
                return channel.writeAndFlush(fixMessage).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            });
        } else {
            return ((AbstractFIXYouNetty) engine).eventLoopGroup.submit(() -> {
                final FixMessage fixMessage = (FixMessage) ((AbstractFIXYouNetty) engine).fixYouNettyComponent.fixMessageObjectPool().getAndRetain();
                fixMessageCreator.accept(fixMessage);
                sessionState.queueMessage(fixMessage);
            });
        }
    }

    @Nonnull
    public static Future logoutSession(@Nonnull AbstractFIXYouNetty engine, @Nonnull SessionID sessionID) { //TODO not sure about this API, Future<ChannelFuture> that does not look very well
        final SessionRegistry<NettyHandlerAwareSessionState> sessionRegistry = engine.fixYouNettyComponent.sessionRegistry();
        final NettyHandlerAwareSessionState sessionState = sessionRegistry.getStateForSessionRequired(sessionID);
        final Channel channel = sessionState.getChannel();
        if (channel != null) {
            return channel.eventLoop()
                          .submit(() -> channel.writeAndFlush(FixMessageUtils.toLogoutMessage((FixMessage) engine.fixYouNettyComponent.fixMessageObjectPool().getAndRetain(), null))
                                               .addListener(FixChannelListeners.LOGOUT_SENT)
                                               .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE));
        } else {
            final CompletableFuture<Object> future = new CompletableFuture<>();
            future.completeExceptionally(new FIXYouException("Cannot log out session that has not been started"));
            return future;
        }
    }

    @Nonnull
    public static ObjectPool<FixMessage> fixMessagePool(@Nonnull Engine engine) { //TODO not sure about this API, should message pool be exposed?
        return ((AbstractFIXYouNetty) engine).fixYouNettyComponent.fixMessageObjectPool();
    }
}
