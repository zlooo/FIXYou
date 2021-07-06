package io.github.zlooo.fixyou.fix.commons.session;

import dagger.Module;
import dagger.Provides;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.commons.AbstractPoolableFixMessage;
import io.github.zlooo.fixyou.commons.memory.Region;
import io.github.zlooo.fixyou.commons.pool.ArrayBackedObjectPool;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.parser.model.OffHeapFixMessage;
import io.github.zlooo.fixyou.session.SessionRegistry;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public interface SessionModule {

    @Singleton
    @Provides
    static SessionRegistry provideSessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Provides
    @Singleton
    @Named("fixMessageObjectPool")
    static ObjectPool<? extends AbstractPoolableFixMessage> provideFixMessageObjectPool(FIXYouConfiguration fixYouConfiguration, @Named("regionPool") ObjectPool<Region> regionPool) {
        return new ArrayBackedObjectPool<>(fixYouConfiguration.getFixMessagePoolSize(), () -> new OffHeapFixMessage(regionPool), OffHeapFixMessage.class, fixYouConfiguration.getFixMessagePoolSize());
    }
}
