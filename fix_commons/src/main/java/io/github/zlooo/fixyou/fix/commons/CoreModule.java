package io.github.zlooo.fixyou.fix.commons;

import dagger.Module;
import dagger.Provides;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.commons.memory.Region;
import io.github.zlooo.fixyou.commons.memory.RegionPool;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.fix.commons.config.validator.ConfigValidatorModule;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Clock;

@Module(includes = ConfigValidatorModule.class)
public interface CoreModule {

    @Provides
    @Nullable
    @Singleton
    static Authenticator authenticator() {
        return null; //TODO for now Janusz way, but I'll have to make this configurable, which is possible in dagger :)
    }

    @Provides
    @Singleton
    static Clock provideClock() {
        return Clock.systemUTC();
    }

    @Provides
    @Singleton
    @Named("regionPool")
    static ObjectPool<Region> provideRegionPool(FIXYouConfiguration fixYouConfiguration) {
        return new RegionPool(fixYouConfiguration.getRegionPoolSize(), fixYouConfiguration.getRegionSize());
    }
}
