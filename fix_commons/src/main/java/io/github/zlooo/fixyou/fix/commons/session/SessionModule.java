package io.github.zlooo.fixyou.fix.commons.session;

import dagger.Module;
import dagger.Provides;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.commons.pool.ArrayBackedObjectPool;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.parser.model.FieldCodec;
import io.github.zlooo.fixyou.parser.model.FixMessage;
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
    static ObjectPool provideFixMessageObjectPool(FieldCodec fieldCodec, FIXYouConfiguration fixYouConfiguration) {
        return new ArrayBackedObjectPool<>(fixYouConfiguration.getFixMessagePoolSize(), () -> new FixMessage(fieldCodec), FixMessage.class, fixYouConfiguration.getFixMessagePoolSize());
    }
}
