package io.github.zlooo.fixyou.netty;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.Engine;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.FIXYouException;
import io.github.zlooo.fixyou.fix.commons.config.validator.ConfigValidator;
import io.github.zlooo.fixyou.model.DefaultExtendedFixSpec;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.netty.handler.FixYouNettyComponent;
import io.github.zlooo.fixyou.netty.handler.NettyResettablesSupplier;
import io.github.zlooo.fixyou.session.SessionConfig;
import io.github.zlooo.fixyou.session.SessionID;
import io.github.zlooo.fixyou.session.SessionRegistry;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.ResourceLeakDetector;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.Future;

abstract class AbstractFIXYouNetty implements Engine {

    protected final EventLoopGroup eventLoopGroup;
    protected final FixYouNettyComponent fixYouNettyComponent;
    protected final FIXYouConfiguration fixYouConfiguration;
    protected final ConfigValidator configValidator;

    AbstractFIXYouNetty(FixYouNettyComponent fixYouNettyComponent, FIXYouConfiguration fixYouConfiguration,
                        ConfigValidator configValidator) {
        this.fixYouNettyComponent = fixYouNettyComponent;
        this.fixYouConfiguration = fixYouConfiguration;
        this.eventLoopGroup = new NioEventLoopGroup(fixYouConfiguration.getNumberOfIOThreads());
        this.configValidator = configValidator;
        //netty specific optimalizations
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

    }

    @Nonnull
    @Override
    public Future stop() {
        final io.netty.util.concurrent.Future<?> shutdownGracefully = eventLoopGroup.shutdownGracefully();
        fixYouNettyComponent.retransmissionSubscriberPool().close();
        ((SessionRegistry<NettyHandlerAwareSessionState>) fixYouNettyComponent.sessionRegistry()).getAll().forEach(Closeable::close);
        ((Closeable) fixYouNettyComponent.fixMessageListenerInvoker()).close();
        return shutdownGracefully;
    }

    @Nonnull
    @Override
    public Engine registerSession(@Nonnull SessionID sessionID, @Nonnull FixSpec fixSpec, @Nonnull SessionConfig sessionConfig) {
        final Set<String> errorMessages = configValidator.validateSessionConfig(sessionConfig);
        if (!errorMessages.isEmpty()) {
            throw new FIXYouException("Session configuration validation failed, reason(s): " + String.join(", ", errorMessages));
        }
        final SessionRegistry<NettyHandlerAwareSessionState> sessionRegistry = fixYouNettyComponent.sessionRegistry();
        final NettyResettablesSupplier nettyResettablesSupplier = fixYouNettyComponent.resettableSupplier();
        sessionRegistry.registerExpectedSession(new NettyHandlerAwareSessionState(sessionConfig, sessionID, new DefaultExtendedFixSpec(fixSpec)), nettyResettablesSupplier);
        return this;
    }
}
