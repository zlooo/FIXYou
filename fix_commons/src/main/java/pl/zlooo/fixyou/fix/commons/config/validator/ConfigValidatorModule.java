package pl.zlooo.fixyou.fix.commons.config.validator;

import dagger.Module;
import dagger.Provides;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public interface ConfigValidatorModule {

    @Provides
    @Singleton
    @Named("acceptorConfigValidator")
    static ConfigValidator acceptorConfigValidator() {
        return new AcceptorConfigValidator();
    }

    @Provides
    @Singleton
    @Named("initiatorConfigValidator")
    static ConfigValidator initiatorConfigValidator() {
        return new InitiatorConfigValidator();
    }
}
