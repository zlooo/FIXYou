package pl.zlooo.fixyou.netty.handler.admin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.FixConstants;
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import pl.zlooo.fixyou.parser.model.FixMessage;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
class LogoutHandler implements AdministrativeMessageHandler {

    @Inject
    LogoutHandler() {
    }

    @Override
    public void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx) {
        final NettyHandlerAwareSessionState sessionState = NettyHandlerAwareSessionState.getForChannelContext(ctx);
        log.info("Logout request for session {} received, closing connection", sessionState.getSessionId());
        if (!sessionState.isLogoutSent()) {
            sessionState.setLogoutSent(true);
            fixMessage.retain();
            ctx.writeAndFlush(fixMessage).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.channel().close();
        }
        sessionState.getSessionConfig().getSessionStateListeners().forEach(sessionStateListener -> sessionStateListener.logOut(sessionState));
    }

    @Override
    public char[] supportedMessageType() {
        return FixConstants.LOGOUT;
    }
}

