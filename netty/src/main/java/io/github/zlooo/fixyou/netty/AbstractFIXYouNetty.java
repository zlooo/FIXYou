package io.github.zlooo.fixyou.netty;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.Engine;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.FIXYouException;
import io.github.zlooo.fixyou.fix.commons.DictionaryRepository;
import io.github.zlooo.fixyou.fix.commons.config.validator.ConfigValidator;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.netty.handler.FixYouNettyComponent;
import io.github.zlooo.fixyou.netty.handler.NettyResettablesSupplier;
import io.github.zlooo.fixyou.session.SessionConfig;
import io.github.zlooo.fixyou.session.SessionID;
import io.github.zlooo.fixyou.session.SessionRegistry;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

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
    }

    @Override
    public Future<?> stop() {
        fixYouNettyComponent.retransmissionSubscriberPool().close();
        ((SessionRegistry<NettyHandlerAwareSessionState>) fixYouNettyComponent.sessionRegistry()).getAll().forEach(Closeable::close);
        final io.netty.util.concurrent.Future<?> shutdownGracefully = eventLoopGroup.shutdownGracefully();
        ((Closeable) fixYouNettyComponent.asyncExecutingHandler()).close();
        return shutdownGracefully;
    }

    @Override
    public Engine registerSession(SessionID sessionID, String dictionaryID, SessionConfig sessionConfig) {
        final Set<String> errorMessages = configValidator.validateSessionConfig(sessionConfig);
        if (!errorMessages.isEmpty()) {
            throw new FIXYouException("Session configuration validation failed, reason(s): " + String.join(", ", errorMessages));
        }
        final SessionRegistry<NettyHandlerAwareSessionState> sessionRegistry = fixYouNettyComponent.sessionRegistry();
        final NettyResettablesSupplier nettyResettablesSupplier = fixYouNettyComponent.resettableSupplier();
        final DictionaryRepository.Dictionary dictionary = fixYouNettyComponent.dictionaryRepository().getDictionary(dictionaryID);
        if (dictionary == null) {
            throw new FIXYouException("Could not find dictionary with id " + dictionaryID + ". Are you sure you've registered it?");
        }
        sessionRegistry.registerExpectedSession(new NettyHandlerAwareSessionState(sessionConfig, sessionID, dictionary.getFixMessageObjectPool(), dictionary.getFixSpec()),
                                                nettyResettablesSupplier);
        return this;
    }

    @Override
    public Engine registerSessionAndDictionary(SessionID sessionID, String dictionaryID, FixSpec fixSpec, SessionConfig sessionConfig) {
        fixYouNettyComponent.dictionaryRepository().registerDictionary(dictionaryID, fixSpec);
        registerSession(sessionID, dictionaryID, sessionConfig);
        return this;
    }
}
