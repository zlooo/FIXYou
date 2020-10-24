package io.github.zlooo.fixyou.netty.handler.admin;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
class LogoutHandler implements AdministrativeMessageHandler {

    private static final CharSequence SUPPORTED_MESSAGE_TYPE = String.copyValueOf(FixConstants.LOGOUT);

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
            fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setCharSequenceValue(FixConstants.LOGOUT);
            ctx.writeAndFlush(fixMessage).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.channel().close();
        }
        sessionState.getSessionConfig().getSessionStateListeners().forEach(sessionStateListener -> sessionStateListener.logOut(sessionState));
    }

    @Override
    public CharSequence supportedMessageType() {
        return SUPPORTED_MESSAGE_TYPE;
    }
}

