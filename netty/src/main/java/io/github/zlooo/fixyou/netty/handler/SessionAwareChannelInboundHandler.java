package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.netty.channel.ChannelInboundHandler;

public interface SessionAwareChannelInboundHandler extends ChannelInboundHandler {

    NettyHandlerAwareSessionState getSessionState();
}
