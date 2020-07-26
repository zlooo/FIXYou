package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.parser.FixMessageParser;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@ChannelHandler.Sharable
class MessageDecoder extends ChannelInboundHandlerAdapter implements Resettable {

    private static enum State {
        READY_TO_DECODE,
        DECODING,
        RESUME
    }

    private final FixMessageParser fixMessageParser = new FixMessageParser();
    private final ReadTask readTask = new ReadTask();
    private final ObjectPool<FixMessage> fixMessagePool;
    private State state = State.READY_TO_DECODE;
    private ChannelHandlerContext currentContext;

    public MessageDecoder(ObjectPool<FixMessage> fixMessagePool) {
        this.fixMessagePool = fixMessagePool;
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
            try {
                final boolean resume = state == State.RESUME;
                saveBuffer(in, resume);
                while (fixMessageParser.isParseable() && fixMessageParser.isUnderlyingBufferReadable()) {
                    if (state == State.READY_TO_DECODE || resume) {
                        final FixMessage fixMessage = fixMessagePool.tryGetAndRetain();
                        if (fixMessage == null) {
                            log.trace("No fix messages available in pool");
                            if (!resume) {
                                handleNotResumeStateWithPoolDepletion(ctx, in);
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
            } finally {
                in.release();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void saveBuffer(ByteBuf in, boolean resume) {
        if (resume) {
            if (log.isDebugEnabled()) {
                log.debug("In resume state, adding message to queue, size before add is {}", readTask.messagesToProcess.size());
            }
            addMessageToTaskQueue(in, true);
        } else {
            fixMessageParser.setFixBytes(in);
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

    private void handleNotResumeStateWithPoolDepletion(ChannelHandlerContext ctx, ByteBuf in) {
        state = State.RESUME;
        log.debug("Insufficient capacity in fix message pool");
        addMessageToTaskQueue(in, false);
        if (!readTask.taskScheduled) {
            log.trace("Scheduling execution of read task");
            readTask.taskScheduled = true;
            ctx.channel().eventLoop().execute(readTask);
        }
    }

    private void addMessageToTaskQueue(ByteBuf in, boolean last) {
        final boolean offerResult = last ? readTask.messagesToProcess.offerLast(in) : readTask.messagesToProcess.offerFirst(in);
        if (!offerResult) {
            log.warn("No space left on internal message queue, have to drop one or else boom goes the dynamite");
        } else {
            in.retain();
        }
    }

    @Override
    public void reset() {
        this.state = State.READY_TO_DECODE;
    }

    @ToString(callSuper = true)
    private final class ReadTask implements Runnable {

        //TODO JMH and choose the fastest, simplest, generating lowest amount of garbage queue implementation. No synchronization or thread safety is needed, only subset of Queue methods may be implemented, maybe write your own
        // implementation
        private final Deque<ByteBuf> messagesToProcess = new ArrayDeque<>(DefaultConfiguration.MESSAGE_DECODER_MESSAGES_QUEUE_SIZE);
        private final Deque<ByteBuf> processingQueue = new ArrayDeque<>(DefaultConfiguration.MESSAGE_DECODER_MESSAGES_QUEUE_SIZE);
        private boolean taskScheduled = false;

        @SneakyThrows
        @Override
        public void run() {
            try {
                log.trace("Task starting, state {}", this);
                processingQueue.addAll(messagesToProcess);
                messagesToProcess.clear();
                ByteBuf msg;
                for (int i = 1; (msg = processingQueue.poll()) != null && i <= DefaultConfiguration.MESSAGE_DECODER_MAX_TASK_BATCH_SIZE; i++) {
                    MessageDecoder.this.channelRead(currentContext, msg);
                    if (!messagesToProcess.isEmpty()) {
                        log.trace("Message has been re-added to queue. This means fix message pool still has no available elements so need to wait a bit");
                        messagesToProcess.addAll(processingQueue);
                        processingQueue.clear();
                        break;
                    }
                }
                if (!messagesToProcess.isEmpty()) {
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
