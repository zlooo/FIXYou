package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.FixMessageParser;
import io.github.zlooo.fixyou.parser.model.FieldCodec;
import io.github.zlooo.fixyou.parser.model.FixMessage;
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

    private static enum State {
        READY_TO_DECODE,
        DECODING
    }

    private final FixMessage fixMessage;
    private final ByteBufComposer byteBufComposer = new ByteBufComposer(DefaultConfiguration.BYTE_BUF_COMPOSER_DEFAULT_COMPONENT_NUMBER);
    private final FixMessageParser fixMessageParser;
    private State state = State.READY_TO_DECODE;

    public MessageDecoder(FixSpec fixSpec, FieldCodec fieldCodec) {
        this.fixMessage = new FixMessage(fieldCodec);
        this.fixMessage.setMessageByteSource(byteBufComposer);
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
                        if (state == State.READY_TO_DECODE) {
                            fixMessageParser.startParsing();
                        }
                         fixMessageParser.parseFixMsgBytes();
                        if (fixMessageParser.isDone()) {
                            messageDecoded(ctx);
                        } else {
                            state = State.DECODING;
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
            log.trace("Message after decoding {}", fixMessage.toString(true));
        }
        ctx.fireChannelRead(fixMessage);
        fixMessage.resetAllDataFieldsAndReleaseByteSource();
        state = State.READY_TO_DECODE;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        reset();
        ctx.fireChannelActive();
    }

    @Override
    public void reset() {
        this.state = State.READY_TO_DECODE;
        this.fixMessageParser.reset();
    }
}
