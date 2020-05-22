package io.github.zlooo.fixyou.netty.handler;

import dagger.Module;
import dagger.Provides;
import io.github.zlooo.fixyou.commons.utils.ListUtils;
import io.github.zlooo.fixyou.netty.handler.validation.SessionAwareValidators;
import io.github.zlooo.fixyou.netty.handler.validation.SimpleValidators;
import io.netty.channel.ChannelHandler;

import javax.inject.Singleton;
import java.time.Clock;
import java.util.Collections;

@Module
public interface HandlerModule {

    @Provides
    @Singleton
    @NamedHandler(Handlers.AFTER_SESSION_MESSAGE_VALIDATOR)
    static ChannelHandler provideAfterSessionHandlerMessageValidatorHandler(Clock clock) {
        return new MessageValidationHandler(Collections.singletonList(SimpleValidators.ORIG_SENDING_TIME_PRESENT),
                                            ListUtils.of(SessionAwareValidators.SENDING_TIME_VALIDATOR, SessionAwareValidators.SESSION_ID_VALIDATOR,
                                                         SessionAwareValidators.createSendingTimeValidator(clock),
                                                         SessionAwareValidators.MESSAGE_TYPE_VALIDATOR));
    }

    @Provides
    @Singleton
    @NamedHandler(Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR)
    static ChannelHandler provideBeforeSessionHandlerMessageValidatorHandler() {
        return new MessageValidationHandler(Collections.emptyList(), ListUtils.of(SessionAwareValidators.BEGIN_STRING_VALIDATOR, SessionAwareValidators.BODY_LENGTH_VALIDATOR));
    }
}
