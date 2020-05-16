package pl.zlooo.fixyou.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.Resettable;
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool;
import pl.zlooo.fixyou.parser.FixMessageReader;
import pl.zlooo.fixyou.parser.model.FixMessage;

@Slf4j
@ChannelHandler.Sharable
class MessageDecoder extends ChannelInboundHandlerAdapter implements Resettable {

    private static enum State {
        READY_TO_DECODE,
        DECODING
    }

    private final FixMessageReader fixMessageReader = new FixMessageReader();
    private State state = State.READY_TO_DECODE;
    private DefaultObjectPool<FixMessage> fixMessagePool;

    public MessageDecoder(DefaultObjectPool<FixMessage> fixMessagePool) {
        this.fixMessagePool = fixMessagePool;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            final ByteBuf in = (ByteBuf) msg;
            try {
                fixMessageReader.setFixBytes(in);
                while (fixMessageReader.isParseable() && fixMessageReader.isUnderlyingBufferReadable()) {
                    if (state == State.READY_TO_DECODE) {
                        final FixMessage fixMessage = fixMessagePool.getAndRetain();
                        fixMessageReader.setFixMessage(fixMessage);
                    }
                    fixMessageReader.parseFixMsgBytes();
                    if (fixMessageReader.isDone()) {
                        final FixMessage fixMessage = fixMessageReader.getFixMessage();
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
