package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.parser.FixMessageParser;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
class MessageDecoder extends ChannelInboundHandlerAdapter implements Resettable {

    private static enum State {
        READY_TO_DECODE,
        DECODING
    }

    private final FixMessageParser fixMessageParser = new FixMessageParser();
    private State state = State.READY_TO_DECODE;
    private ObjectPool<FixMessage> fixMessagePool;

    public MessageDecoder(ObjectPool<FixMessage> fixMessagePool) {
        this.fixMessagePool = fixMessagePool;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            final ByteBuf in = (ByteBuf) msg;
            try {
                fixMessageParser.setFixBytes(in);
                while (fixMessageParser.isParseable() && fixMessageParser.isUnderlyingBufferReadable()) {
                    if (state == State.READY_TO_DECODE) {
                        final FixMessage fixMessage = fixMessagePool.getAndRetain();
                        fixMessageParser.setFixMessage(fixMessage);
                    }
                    fixMessageParser.parseFixMsgBytes();
                    if (fixMessageParser.isDone()) {
                        final FixMessage fixMessage = fixMessageParser.getFixMessage();
                        log.trace("Message after decoding {}", fixMessage);
                        ctx.fireChannelRead(fixMessage);
                        state = State.READY_TO_DECODE;
                    } else {
                        state = State.DECODING;
                    }
                }
            } finally {
                in.release();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void reset() {
        this.state = State.READY_TO_DECODE;
    }
}
