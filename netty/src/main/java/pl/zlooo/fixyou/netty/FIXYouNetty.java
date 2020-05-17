package pl.zlooo.fixyou.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import lombok.experimental.UtilityClass;
import pl.zlooo.fixyou.Engine;
import pl.zlooo.fixyou.FIXYouConfiguration;
import pl.zlooo.fixyou.FIXYouException;
import pl.zlooo.fixyou.fix.commons.FixMessageListener;
import pl.zlooo.fixyou.fix.commons.config.validator.ConfigValidator;
import pl.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import pl.zlooo.fixyou.netty.handler.DaggerFixYouNettyComponent;
import pl.zlooo.fixyou.netty.handler.FixYouNettyComponent;
import pl.zlooo.fixyou.netty.utils.FixChannelListeners;
import pl.zlooo.fixyou.parser.model.FixMessage;
import pl.zlooo.fixyou.session.SessionID;
import pl.zlooo.fixyou.session.SessionRegistry;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

@UtilityClass
public class FIXYouNetty {

    @Nonnull
    public static <T extends FixMessageListener> Engine create(@Nonnull FIXYouConfiguration fixYouConfiguration, @Nonnull T fixMessageListener) {
        final FixYouNettyComponent fixYouNettyComponent = DaggerFixYouNettyComponent.builder().fixMessageListener(fixMessageListener).build();
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
    public static Future sendMessage(@Nonnull Consumer<FixMessage> fixMessageCreator, @Nonnull SessionID sessionID, @Nonnull AbstractFIXYouNetty engine) { //TODO not sure about this API, Future<ChannelFuture> that does not look very well
        final SessionRegistry<NettyHandlerAwareSessionState> sessionRegistry = engine.fixYouNettyComponent.sessionRegistry();
        final NettyHandlerAwareSessionState sessionState = sessionRegistry.getStateForSessionRequired(sessionID);
        final Channel channel = sessionState.getChannel();
        if (channel != null) {
            return channel.eventLoop().submit(() -> {
                final FixMessage fixMessage = sessionState.getFixMessageObjectPool().getAndRetain();
                fixMessageCreator.accept(fixMessage);
                return channel.writeAndFlush(fixMessage).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            });
        } else {
            return engine.eventLoopGroup.submit(() -> {
                final FixMessage fixMessage = sessionState.getFixMessageObjectPool().getAndRetain();
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
                          .submit(() -> channel.writeAndFlush(FixMessageUtils.toLogoutMessage(sessionState.getFixMessageObjectPool().getAndRetain(), null))
                                               .addListener(FixChannelListeners.LOGOUT_SENT)
                                               .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE));
        } else {
            final CompletableFuture<Object> future = new CompletableFuture<>();
            future.completeExceptionally(new FIXYouException("Cannot log out session that has not been started"));
            return future;
        }
    }
}
