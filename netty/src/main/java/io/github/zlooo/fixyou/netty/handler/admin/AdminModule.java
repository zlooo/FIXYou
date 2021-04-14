package io.github.zlooo.fixyou.netty.handler.admin;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.commons.pool.ArrayBackedObjectPool;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Module
public interface AdminModule {

    @Provides
    @ElementsIntoSet
    @Singleton
    static Set<AdministrativeMessageHandler> provideHandlers(LogonHandler logonHandler, ResendRequestHandler resendRequestHandler, SequenceResetGapFillModeHandler sequenceResetGapFillModeHandler, LogoutHandler logoutHandler,
                                                             HeartbeatHandler heartbeatHandler, TestRequestHandler testRequestHandler, RejectHandler rejectHandler) {
        final Set<AdministrativeMessageHandler> elements = new HashSet<>();
        elements.add(logonHandler);
        elements.add(logoutHandler);
        elements.add(resendRequestHandler);
        elements.add(sequenceResetGapFillModeHandler);
        elements.add(heartbeatHandler);
        elements.add(testRequestHandler);
        elements.add(rejectHandler);
        return elements;
    }

    @Provides
    @Singleton
    @Named("retransmissionSubscriberPool")
    static ObjectPool provideFixMessageSubscriberPool() {
        return new ArrayBackedObjectPool<>(DefaultConfiguration.FIX_MESSAGE_SUBSCRIBER_POOL_SIZE, RetransmitionSubscriber::new, RetransmitionSubscriber.class, DefaultConfiguration.FIX_MESSAGE_SUBSCRIBER_POOL_SIZE);
    }
}
