package pl.zlooo.fixyou.netty.utils;

import io.netty.channel.ChannelFutureListener;
import lombok.experimental.UtilityClass;
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState;

@UtilityClass
public class FixChannelListeners {

    public static final ChannelFutureListener LOGOUT_SENT = future -> {
        if (future.isSuccess()) {
            NettyHandlerAwareSessionState.getForChannel(future.channel()).setLogoutSent(true);
        }
    };
    public static final ChannelFutureListener LOGON_SENT = future -> {
        if (future.isSuccess()) {
            NettyHandlerAwareSessionState.getForChannel(future.channel()).setLogonSent(true);
        }
    };
}
