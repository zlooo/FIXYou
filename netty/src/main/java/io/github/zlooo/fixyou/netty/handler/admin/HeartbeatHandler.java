package io.github.zlooo.fixyou.netty.handler.admin;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
class HeartbeatHandler implements AdministrativeMessageHandler {

    @Inject
    HeartbeatHandler() {
    }

    @Override
    public void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx) {
        //nothing to do
    }

    @Override
    public char[] supportedMessageType() {
        return FixConstants.HEARTBEAT;
    }
}
