package pl.zlooo.fixyou.fix.commons;

import dagger.Module;
import dagger.Provides;
import pl.zlooo.fixyou.fix.commons.config.validator.ConfigValidatorModule;

import javax.annotation.Nullable;
import java.time.Clock;

@Module(includes = ConfigValidatorModule.class)
public interface CoreModule {

    @Provides
    @Nullable
    static Authenticator authenticator() {
        return null; //TODO for now Janusz way, but I'll have to make this configurable, which is possible in dagger :)
    }

    @Provides
    static Clock provideClock() {
        return Clock.systemUTC();
    }
}
