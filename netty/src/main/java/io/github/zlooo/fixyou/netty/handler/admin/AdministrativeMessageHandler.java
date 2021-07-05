package io.github.zlooo.fixyou.netty.handler.admin;

import io.github.zlooo.fixyou.model.FixMessage;
import io.netty.channel.ChannelHandlerContext;

public interface AdministrativeMessageHandler {

    void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx);

    CharSequence supportedMessageType();
}
