package pl.zlooo.fixyou.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import pl.zlooo.fixyou.Closeable;
import pl.zlooo.fixyou.Engine;
import pl.zlooo.fixyou.FIXYouConfiguration;
import pl.zlooo.fixyou.FIXYouException;
import pl.zlooo.fixyou.fix.commons.DictionaryRepository;
import pl.zlooo.fixyou.fix.commons.config.validator.ConfigValidator;
import pl.zlooo.fixyou.model.FixSpec;
import pl.zlooo.fixyou.netty.handler.FixYouNettyComponent;
import pl.zlooo.fixyou.netty.handler.NettyResettablesSupplier;
import pl.zlooo.fixyou.session.SessionConfig;
import pl.zlooo.fixyou.session.SessionID;
import pl.zlooo.fixyou.session.SessionRegistry;

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
        return eventLoopGroup.shutdownGracefully();
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
