package io.github.zlooo.fixyou.netty.handler;

import dagger.BindsInstance;
import dagger.Component;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.fix.commons.CoreModule;
import io.github.zlooo.fixyou.fix.commons.DictionaryRepository;
import io.github.zlooo.fixyou.fix.commons.FixMessageListener;
import io.github.zlooo.fixyou.fix.commons.config.validator.ConfigValidator;
import io.github.zlooo.fixyou.fix.commons.session.SessionModule;
import io.github.zlooo.fixyou.netty.handler.admin.AdminModule;
import io.github.zlooo.fixyou.session.SessionRegistry;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Component(modules = {CoreModule.class, SessionModule.class, AdminModule.class, HandlerModule.class})
public interface FixYouNettyComponent {

    FIXYouChannelInitializer channelInitializer();

    SessionRegistry sessionRegistry();

    NettyResettablesSupplier resettableSupplier();

    DictionaryRepository dictionaryRepository();

    @NamedHandler(Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR)
    ChannelHandler beforeSessionMessageValidatorHandler();

    @NamedHandler(Handlers.AFTER_SESSION_MESSAGE_VALIDATOR)
    ChannelHandler afterSessionMessageValidatorHandler();

    @NamedHandler(Handlers.ASYNC_EXECUTING_HANDLER)
    ChannelInboundHandler asyncExecutingHandler();

    @Named("acceptorConfigValidator")
    ConfigValidator acceptorConfigValidator();

    @Named("initiatorConfigValidator")
    ConfigValidator initiatorConfigValidator();

    @Named("retransmissionSubscriberPool")
    ObjectPool retransmissionSubscriberPool();

    @Component.Builder
    interface Builder {

        @BindsInstance
        Builder fixMessageListener(FixMessageListener fixMessageListener);

        @BindsInstance
        Builder fixYouConfiguration(FIXYouConfiguration fixYouConfiguration);

        FixYouNettyComponent build();
    }
}
