package io.github.zlooo.fixyou.netty.handler.admin;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.model.FixMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
class HeartbeatHandler implements AdministrativeMessageHandler {

    private static final CharSequence SUPPORTED_MESSAGE_TYPE = String.copyValueOf(FixConstants.HEARTBEAT);

    @Inject
    HeartbeatHandler() {
    }

    @Override
    public void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx) {
        //nothing to do
    }

    @Override
    public CharSequence supportedMessageType() {
        return SUPPORTED_MESSAGE_TYPE;
    }
}
