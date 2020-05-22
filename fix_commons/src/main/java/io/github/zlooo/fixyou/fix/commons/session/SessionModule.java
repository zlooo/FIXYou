package io.github.zlooo.fixyou.fix.commons.session;

import dagger.Module;
import dagger.Provides;
import io.github.zlooo.fixyou.session.SessionRegistry;

import javax.inject.Singleton;

@Module
public interface SessionModule {

    @Singleton
    @Provides
    static SessionRegistry provideSessionRegistry() {
        return new SessionRegistryImpl();
    }
}
