package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.fix.commons.FixMessageListener;
import io.github.zlooo.fixyou.netty.AbstractNettyAwareFixMessageListener;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
class FixMessageListenerInvokingHandler extends SimpleChannelInboundHandler<FixMessage> {

    private final FixMessageListener fixMessageListener;

    @Inject
    FixMessageListenerInvokingHandler(FixMessageListener fixMessageListener) {
        this.fixMessageListener = fixMessageListener;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (fixMessageListener instanceof AbstractNettyAwareFixMessageListener) {
            ((AbstractNettyAwareFixMessageListener) fixMessageListener).setChannel(ctx.channel());
        }
        ctx.fireChannelActive();
    }

    //TODO add option to invoke in disruptor!!!!
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FixMessage msg) throws Exception {
        fixMessageListener.onFixMessage(NettyHandlerAwareSessionState.getSessionID(ctx), msg);
    }
}
