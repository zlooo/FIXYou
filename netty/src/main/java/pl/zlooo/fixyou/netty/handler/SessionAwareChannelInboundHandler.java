package pl.zlooo.fixyou.netty.handler;

import io.netty.channel.ChannelInboundHandler;
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState;

public interface SessionAwareChannelInboundHandler extends ChannelInboundHandler {

    NettyHandlerAwareSessionState getSessionState();
}
