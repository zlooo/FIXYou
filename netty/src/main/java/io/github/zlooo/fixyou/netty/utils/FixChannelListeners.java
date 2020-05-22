package io.github.zlooo.fixyou.netty.utils;

import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.netty.channel.ChannelFutureListener;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FixChannelListeners {

    public static final ChannelFutureListener LOGOUT_SENT = future -> {
        if (future.isSuccess()) {
            final NettyHandlerAwareSessionState sessionState = NettyHandlerAwareSessionState.getForChannel(future.channel());
            sessionState.setLogoutSent(true);
            sessionState.getSessionConfig().getSessionStateListeners().forEach(listener -> listener.logOut(sessionState));
        }
    };
    public static final ChannelFutureListener LOGON_SENT = future -> {
        if (future.isSuccess()) {
            NettyHandlerAwareSessionState.getForChannel(future.channel()).setLogonSent(true);
        }
    };
}
