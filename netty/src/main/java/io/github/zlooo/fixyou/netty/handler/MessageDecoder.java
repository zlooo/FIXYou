package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.FixMessageParser;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;
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

    private final ReadTask readTask = new ReadTask();
    private final ObjectPool<FixMessage> fixMessagePool;
    private final ByteBufComposer byteBufComposer = new ByteBufComposer(DefaultConfiguration.BYTE_BUF_COMPOSER_DEFAULT_COMPONENT_NUMBER);
    private final FixMessageParser fixMessageParser;
    private State state = State.READY_TO_DECODE;
    private ChannelHandlerContext currentContext;

    public MessageDecoder(ObjectPool<FixMessage> fixMessagePool, FixSpec fixSpec) {
        this.fixMessagePool = fixMessagePool;
        this.fixMessageParser = new FixMessageParser(byteBufComposer, fixSpec);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        currentContext = ctx;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        currentContext = null;
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
                    tryToParseMessages(ctx);
                } finally {
                    in.release();
                }
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void tryToParseMessages(ChannelHandlerContext ctx) {
        while (fixMessageParser.canContinueParsing()) {
            if (state == State.READY_TO_DECODE) {
                final FixMessage fixMessage = fixMessagePool.tryGetAndRetain();
                if (fixMessage == null) {
                    log.trace("No fix messages available in pool");
                    if (!readTask.taskScheduled) {
                        log.trace("Scheduling execution of read task");
                        readTask.taskScheduled = true;
                        ctx.channel().eventLoop().execute(readTask);
                    }
                    return;
                }
                fixMessageParser.setFixMessage(fixMessage);
            }
            fixMessageParser.parseFixMsgBytes();
            if (fixMessageParser.isDone()) {
                messageDecoded(ctx);
            } else {
                state = State.DECODING;
            }
        }
    }

    private void messageDecoded(ChannelHandlerContext ctx) {
        final FixMessage fixMessage = fixMessageParser.getFixMessage();
        if (log.isTraceEnabled()) {
            log.trace("Message after decoding {}", fixMessage.toString(true));
        }
        ctx.fireChannelRead(fixMessage);
        fixMessageParser.setFixMessage(null);
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

    @ToString(callSuper = true)
    private final class ReadTask implements Runnable {
        private boolean taskScheduled;

        @SneakyThrows
        @Override
        public void run() {
            try {
                log.trace("Task starting, state {}", this);
                tryToParseMessages(currentContext);
                if (byteBufComposer.readerIndex() < byteBufComposer.getStoredEndIndex()) {
                    log.trace("Messages to process queue still not empty, scheduling next execution");
                    taskScheduled = true;
                    //TODO think about some backoff?
                    currentContext.channel().eventLoop().execute(this);
                } else {
                    taskScheduled = false;
                }
            } finally {
                log.trace("Task {} done", this);
            }
        }
    }
}
