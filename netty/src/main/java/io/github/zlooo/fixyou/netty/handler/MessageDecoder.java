package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.commons.memory.Region;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.FixMessageParser;
import io.github.zlooo.fixyou.parser.model.OffHeapFixMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
@ToString
class MessageDecoder extends ChannelInboundHandlerAdapter implements Resettable {

    private final FixMessage fixMessage;
    private final ByteBufComposer byteBufComposer = new ByteBufComposer(DefaultConfiguration.BYTE_BUF_COMPOSER_DEFAULT_COMPONENT_NUMBER);
    private final FixMessageParser fixMessageParser;
    private final FixSpec fixSpec;

    public MessageDecoder(FixSpec fixSpec, ObjectPool<Region> regionObjectPool) {
        this.fixSpec = fixSpec;
        this.fixMessage = new OffHeapFixMessage(regionObjectPool);
        this.fixMessageParser = new FixMessageParser(byteBufComposer, fixSpec, fixMessage);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            final ByteBuf in = (ByteBuf) msg;
            if (!byteBufComposer.addByteBuf(in)) {
                final Channel channel = ctx.channel();
                log.error("ByteBufComposer for channel {} is full, disconnecting it. Consider expanding size of ByteBufComposer for session {}", channel, NettyHandlerAwareSessionState.getForChannel(channel).getSessionId());
                channel.disconnect();
            } else {
                try {
                    while (fixMessageParser.canContinueParsing()) {
                        fixMessageParser.parseFixMsgBytes();
                        if (fixMessageParser.isDone()) {
                            messageDecoded(ctx);
                        }
                    }
                } finally {
                    in.release();
                }
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void messageDecoded(ChannelHandlerContext ctx) {
        if (log.isTraceEnabled()) {
            log.trace("Message after decoding {}", fixMessage.toString(true, fixSpec));
        }
        byteBufComposer.releaseData(0, byteBufComposer.readerIndex() - 1);
        ctx.fireChannelRead(fixMessage);
        fixMessage.reset();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        reset();
        ctx.fireChannelActive();
    }

    @Override
    public void reset() {
        this.fixMessageParser.reset();
    }
}
