package pl.zlooo.fixyou.netty.handler;

import dagger.BindsInstance;
import dagger.Component;
import io.netty.channel.ChannelHandler;
import pl.zlooo.fixyou.commons.pool.ObjectPool;
import pl.zlooo.fixyou.fix.commons.CoreModule;
import pl.zlooo.fixyou.fix.commons.DictionaryRepository;
import pl.zlooo.fixyou.fix.commons.FixMessageListener;
import pl.zlooo.fixyou.fix.commons.config.validator.ConfigValidator;
import pl.zlooo.fixyou.fix.commons.session.SessionModule;
import pl.zlooo.fixyou.netty.handler.admin.AdminModule;
import pl.zlooo.fixyou.session.SessionRegistry;

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

        FixYouNettyComponent build();
    }
}
