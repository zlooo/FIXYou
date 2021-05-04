package io.github.zlooo.fixyou.netty.handler;

import dagger.Module;
import dagger.Provides;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.commons.memory.Region;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.commons.utils.ListUtils;
import io.github.zlooo.fixyou.fix.commons.FixMessageListener;
import io.github.zlooo.fixyou.netty.handler.validation.SessionAwareValidators;
import io.github.zlooo.fixyou.netty.handler.validation.SimpleValidators;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.channel.ChannelHandler;

import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Clock;
import java.util.Collections;

@Module
public interface HandlerModule {

    @Provides
    @Singleton
    @NamedHandler(Handlers.AFTER_SESSION_MESSAGE_VALIDATOR)
    static ChannelHandler provideAfterSessionHandlerMessageValidatorHandler(Clock clock, @Named("fixMessageObjectPool") ObjectPool<FixMessage> fixMessageObjectPool) {
        return new MessageValidationHandler(Collections.singletonList(SimpleValidators.ORIG_SENDING_TIME_PRESENT),
                                            ListUtils.of(SessionAwareValidators.ORIG_SENDING_TIME_VALIDATOR, SessionAwareValidators.BEGIN_STRING_VALIDATOR, SessionAwareValidators.COMP_ID_VALIDATOR,
                                                         SessionAwareValidators.createSendingTimeValidator(clock), SessionAwareValidators.MESSAGE_TYPE_VALIDATOR), fixMessageObjectPool);
    }

    @Provides
    @Singleton
    @NamedHandler(Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR)
    static ChannelHandler provideBeforeSessionHandlerMessageValidatorHandler(@Named("fixMessageObjectPool") ObjectPool<FixMessage> fixMessageObjectPool) {
        return new MessageValidationHandler(Collections.emptyList(), ListUtils.of(SessionAwareValidators.BODY_LENGTH_VALIDATOR), fixMessageObjectPool);
    }

    @Provides
    @Singleton
    @NamedHandler(Handlers.LISTENER_INVOKER)
    static ChannelHandler provideFixMessageListenerInvoker(FixMessageListener fixMessageListener, FIXYouConfiguration configuration, @Named("regionPool") ObjectPool<Region> regionPool) {
        return new FixMessageListenerInvokingHandler(fixMessageListener, configuration, regionPool);
    }
}
