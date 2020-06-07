package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Heavily based on {@link io.netty.handler.timeout.IdleStateHandler}. Differences:
 * 1) It's session aware
 * 2) When timeout is hit it just sends heartbeat instead of IdleStateEvent
 */
@Slf4j
@ChannelHandler.Sharable
//TODO I'm not sure I'm ok with this class being public :/
//TODO remove checkstyle suppressions from this class
public class MutableIdleStateHandler extends ChannelDuplexHandler implements SessionAwareChannelInboundHandler, Resettable {
    private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1);
    private static final char[] TEST_REQ_ID = {'t', 'e', 's', 't'};

    // Not create a new ChannelFutureListener per write operation to reduce GC pressure.
    private final ChannelFutureListener writeListener = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            lastWriteTime = ticksInNanos();
            firstWriterIdleEvent = true;
            firstAllIdleEvent = true;
        }
    };

    private final boolean observeOutput;
    private final NettyHandlerAwareSessionState sessionState;
    private long readerIdleTimeNanos;
    private long writerIdleTimeNanos;
    private long allIdleTimeNanos;

    private ScheduledFuture<?> readerIdleTimeout;
    private long lastReadTime;
    private boolean firstReaderIdleEvent = true;

    private ScheduledFuture<?> writerIdleTimeout;
    private long lastWriteTime;
    private boolean firstWriterIdleEvent = true;

    private ScheduledFuture<?> allIdleTimeout;
    private boolean firstAllIdleEvent = true;

    private byte state; // 0 - none, 1 - initialized, 2 - destroyed
    private boolean reading;

    private long lastChangeCheckTimeStamp;
    private int lastMessageHashCode;
    private long lastPendingWriteBytes;
    private long lastFlushProgress;
    private EventLoop eventLoop;

    public MutableIdleStateHandler(NettyHandlerAwareSessionState sessionState) {
        this(sessionState, false, 0, 0, 0, TimeUnit.NANOSECONDS);
    }

    /**
     * Creates a new instance firing {@link IdleStateEvent}s.
     *
     * @param readerIdleTimeSeconds an {@link IdleStateEvent} whose state is {@link IdleState#READER_IDLE}
     *                              will be triggered when no read was performed for the specified
     *                              period of time.  Specify {@code 0} to disable.
     * @param writerIdleTimeSeconds an {@link IdleStateEvent} whose state is {@link IdleState#WRITER_IDLE}
     *                              will be triggered when no write was performed for the specified
     *                              period of time.  Specify {@code 0} to disable.
     * @param allIdleTimeSeconds    an {@link IdleStateEvent} whose state is {@link IdleState#ALL_IDLE}
     *                              will be triggered when neither read nor write was performed for
     *                              the specified period of time.  Specify {@code 0} to disable.
     */
    public MutableIdleStateHandler(NettyHandlerAwareSessionState sessionState, int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds) {

        this(sessionState, readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds, TimeUnit.SECONDS);
    }

    /**
     * @see #MutableIdleStateHandler(NettyHandlerAwareSessionState, boolean, long, long, long, TimeUnit)
     */
    public MutableIdleStateHandler(NettyHandlerAwareSessionState sessionState, long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
        this(sessionState, false, readerIdleTime, writerIdleTime, allIdleTime, unit);
    }

    /**
     * Creates a new instance firing {@link IdleStateEvent}s.
     *
     * @param observeOutput  whether or not the consumption of {@code bytes} should be taken into
     *                       consideration when assessing write idleness. The default is {@code false}.
     * @param readerIdleTime an {@link IdleStateEvent} whose state is {@link IdleState#READER_IDLE}
     *                       will be triggered when no read was performed for the specified
     *                       period of time.  Specify {@code 0} to disable.
     * @param writerIdleTime an {@link IdleStateEvent} whose state is {@link IdleState#WRITER_IDLE}
     *                       will be triggered when no write was performed for the specified
     *                       period of time.  Specify {@code 0} to disable.
     * @param allIdleTime    an {@link IdleStateEvent} whose state is {@link IdleState#ALL_IDLE}
     *                       will be triggered when neither read nor write was performed for
     *                       the specified period of time.  Specify {@code 0} to disable.
     * @param unit           the {@link TimeUnit} of {@code readerIdleTime},
     *                       {@code writeIdleTime}, and {@code allIdleTime}
     */
    public MutableIdleStateHandler(NettyHandlerAwareSessionState sessionState, boolean observeOutput, long readerIdleTime, long writerIdleTime, long allIdleTime, TimeUnit unit) {
        this.sessionState = sessionState;
        ObjectUtil.checkNotNull(unit, "unit");

        this.observeOutput = observeOutput;

        if (readerIdleTime <= 0) {
            readerIdleTimeNanos = 0;
        } else {
            readerIdleTimeNanos = Math.max(unit.toNanos(readerIdleTime), MIN_TIMEOUT_NANOS);
        }
        if (writerIdleTime <= 0) {
            writerIdleTimeNanos = 0;
        } else {
            writerIdleTimeNanos = Math.max(unit.toNanos(writerIdleTime), MIN_TIMEOUT_NANOS);
        }
        if (allIdleTime <= 0) {
            allIdleTimeNanos = 0;
        } else {
            allIdleTimeNanos = Math.max(unit.toNanos(allIdleTime), MIN_TIMEOUT_NANOS);
        }
    }

    /**
     * Return the readerIdleTime that was given when instance this class in milliseconds.
     */
    public long getReaderIdleTimeInMillis() {
        return TimeUnit.NANOSECONDS.toMillis(readerIdleTimeNanos);
    }

    /**
     * Return the writerIdleTime that was given when instance this class in milliseconds.
     */
    public long getWriterIdleTimeInMillis() {
        return TimeUnit.NANOSECONDS.toMillis(writerIdleTimeNanos);
    }

    /**
     * Return the allIdleTime that was given when instance this class in milliseconds.
     */
    public long getAllIdleTimeInMillis() {
        return TimeUnit.NANOSECONDS.toMillis(allIdleTimeNanos);
    }

    public void setReaderIdleTimeNanos(long readerIdleTimeNanos) {
        this.readerIdleTimeNanos = readerIdleTimeNanos;
    }

    public void setWriterIdleTimeNanos(long writerIdleTimeNanos) {
        this.writerIdleTimeNanos = writerIdleTimeNanos;
    }

    public void setAllIdleTimeNanos(long allIdleTimeNanos) {
        this.allIdleTimeNanos = allIdleTimeNanos;
    }

    @Override
    public NettyHandlerAwareSessionState getSessionState() {
        return sessionState;
    }

    @Override
    public void reset() {
        destroy();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            // channelActive() event has been fired already, which means this.channelActive() will
            // not be invoked. We have to initialize here instead.
            initialize(ctx);
        } else {
            // channelActive() event has not been fired yet.  this.channelActive() will be invoked
            // and initialization will occur there.
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        destroy();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // Initialize early if channel is active already.
        if (ctx.channel().isActive()) {
            initialize(ctx);
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // This method will be invoked only if this handler was added
        // before channelActive() event is fired.  If a user adds this handler
        // after the channelActive() event, initialize() will be called by beforeAdd().
        initialize(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        destroy();
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (readerIdleTimeNanos > 0 || allIdleTimeNanos > 0) {
            reading = true;
            firstReaderIdleEvent = true;
            firstAllIdleEvent = true;
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if ((readerIdleTimeNanos > 0 || allIdleTimeNanos > 0) && reading) {
            lastReadTime = ticksInNanos();
            reading = false;
        }
        ctx.fireChannelReadComplete();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // Allow writing with void promise if handler is only configured for read timeout events.
        if (writerIdleTimeNanos > 0 || allIdleTimeNanos > 0) {
            ctx.write(msg, promise.unvoid()).addListener(writeListener);
        } else {
            ctx.write(msg, promise);
        }
    }

    private void initialize(ChannelHandlerContext ctx) {
        eventLoop = ctx.channel().eventLoop();
        // Avoid the case where destroy() is called before scheduling timeouts.
        // See: https://github.com/netty/netty/issues/143
        switch (state) {
            case 1:
            case 2:
                return;
            default:
                //nothing to do
        }

        state = 1;
        initOutputChanged(ctx);

        lastReadTime = ticksInNanos();
        lastWriteTime = ticksInNanos();
        if (readerIdleTimeNanos > 0) {
            readerIdleTimeout = schedule(ctx, new MutableIdleStateHandler.ReaderIdleTimeoutTask(ctx),
                                         readerIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
        if (writerIdleTimeNanos > 0) {
            writerIdleTimeout = schedule(ctx, new MutableIdleStateHandler.WriterIdleTimeoutTask(ctx),
                                         writerIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
        if (allIdleTimeNanos > 0) {
            allIdleTimeout = schedule(ctx, new MutableIdleStateHandler.AllIdleTimeoutTask(ctx),
                                      allIdleTimeNanos, TimeUnit.NANOSECONDS);
        }
    }

    /**
     * This method is visible for testing!
     */
    long ticksInNanos() {
        return System.nanoTime();
    }

    /**
     * This method is visible for testing!
     */
    ScheduledFuture<?> schedule(ChannelHandlerContext ctx, Runnable task, long delay, TimeUnit unit) {
        if (eventLoop != null) {
            return eventLoop.schedule(task, delay, unit);
        } else {
            return ctx.executor().schedule(task, delay, unit);
        }
    }

    private void destroy() {
        eventLoop = null;
        state = 2;

        if (readerIdleTimeout != null) {
            readerIdleTimeout.cancel(false);
            readerIdleTimeout = null;
        }
        if (writerIdleTimeout != null) {
            writerIdleTimeout.cancel(false);
            writerIdleTimeout = null;
        }
        if (allIdleTimeout != null) {
            allIdleTimeout.cancel(false);
            allIdleTimeout = null;
        }
    }

    /**
     * Is called when an {@link IdleStateEvent} should be fired. This implementation calls
     * {@link ChannelHandlerContext#fireUserEventTriggered(Object)}.
     */
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        final IdleState idleState = evt.state();
        switch (idleState) {
            case WRITER_IDLE:
                log.info("Connection idle for too long, sending heartbeat for session {}", sessionState.getSessionId());
                ctx.writeAndFlush(FixMessageUtils.toHeartbeatMessage(sessionState.getFixMessageObjectPool().getAndRetain())).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                break;
            case READER_IDLE:
                if (evt.isFirst()) {
                    log.info("Connection idle for too long, sending test request for session {}", sessionState.getSessionId());
                    //TODO some randomness in test request id?
                    ctx.writeAndFlush(FixMessageUtils.toTestRequest(sessionState.getFixMessageObjectPool().getAndRetain(), TEST_REQ_ID)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                } else {
                    log.warn("Second read timeout, closing connection for session {}", sessionState.getSessionId());
                    ctx.close();
                }
                break;
            case ALL_IDLE:
                break;
            default:
                throw new IllegalArgumentException("Unexpected IdleStateEvent, expected read, write or all but got " + idleState);
        }
    }

    /**
     * Returns a {@link IdleStateEvent}.
     */
    protected IdleStateEvent newIdleStateEvent(IdleState currentState, boolean first) {
        switch (currentState) {
            case ALL_IDLE:
                return first ? IdleStateEvent.FIRST_ALL_IDLE_STATE_EVENT : IdleStateEvent.ALL_IDLE_STATE_EVENT;
            case READER_IDLE:
                return first ? IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT : IdleStateEvent.READER_IDLE_STATE_EVENT;
            case WRITER_IDLE:
                return first ? IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT : IdleStateEvent.WRITER_IDLE_STATE_EVENT;
            default:
                throw new IllegalArgumentException("Unhandled: state=" + currentState + ", first=" + first);
        }
    }

    /**
     * @see #hasOutputChanged(ChannelHandlerContext, boolean)
     */
    private void initOutputChanged(ChannelHandlerContext ctx) {
        if (observeOutput) {
            final Channel channel = ctx.channel();
            final Channel.Unsafe unsafe = channel.unsafe();
            final ChannelOutboundBuffer buf = unsafe.outboundBuffer();

            if (buf != null) {
                lastMessageHashCode = System.identityHashCode(buf.current());
                lastPendingWriteBytes = buf.totalPendingWriteBytes();
                lastFlushProgress = buf.currentProgress();
            }
        }
    }

    /**
     * Returns {@code true} if and only if the {@link IdleStateHandler} was constructed
     * with {@link #observeOutput} enabled and there has been an observed change in the
     * {@link ChannelOutboundBuffer} between two consecutive calls of this method.
     * <p>
     * https://github.com/netty/netty/issues/6150
     */
    private boolean hasOutputChanged(ChannelHandlerContext ctx, boolean first) {
        if (observeOutput) {

            // We can take this shortcut if the ChannelPromises that got passed into write()
            // appear to complete. It indicates "change" on message level and we simply assume
            // that there's change happening on byte level. If the user doesn't observe channel
            // writability events then they'll eventually OOME and there's clearly a different
            // problem and idleness is least of their concerns.
            if (lastChangeCheckTimeStamp != lastWriteTime) {
                lastChangeCheckTimeStamp = lastWriteTime;

                // But this applies only if it's the non-first call.
                if (!first) {
                    return true;
                }
            }

            final Channel channel = ctx.channel();
            final Channel.Unsafe unsafe = channel.unsafe();
            final ChannelOutboundBuffer buf = unsafe.outboundBuffer();

            if (buf != null) {
                final int messageHashCode = System.identityHashCode(buf.current());
                final long pendingWriteBytes = buf.totalPendingWriteBytes();

                if (messageHashCode != lastMessageHashCode || pendingWriteBytes != lastPendingWriteBytes) {
                    lastMessageHashCode = messageHashCode;
                    lastPendingWriteBytes = pendingWriteBytes;

                    if (!first) {
                        return true;
                    }
                }

                final long flushProgress = buf.currentProgress();
                if (flushProgress != lastFlushProgress) {
                    lastFlushProgress = flushProgress;

                    if (!first) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private abstract static class AbstractIdleTask implements Runnable {

        private final ChannelHandlerContext ctx;

        AbstractIdleTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            if (!ctx.channel().isOpen()) {
                return;
            }

            run(ctx);
        }

        protected abstract void run(ChannelHandlerContext channelHandlerContext);
    }

    private final class ReaderIdleTimeoutTask extends MutableIdleStateHandler.AbstractIdleTask {

        ReaderIdleTimeoutTask(ChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        protected void run(ChannelHandlerContext ctx) {
            long nextDelay = readerIdleTimeNanos;
            if (!reading) {
                nextDelay -= ticksInNanos() - lastReadTime;
            }

            if (nextDelay <= 0) {
                // Reader is idle - set a new timeout and notify the callback.
                readerIdleTimeout = schedule(ctx, this, readerIdleTimeNanos, TimeUnit.NANOSECONDS);

                final boolean first = firstReaderIdleEvent;
                firstReaderIdleEvent = false;

                try {
                    final IdleStateEvent event = newIdleStateEvent(IdleState.READER_IDLE, first);
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                // Read occurred before the timeout - set a new timeout with shorter delay.
                readerIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }

    private final class WriterIdleTimeoutTask extends MutableIdleStateHandler.AbstractIdleTask {

        WriterIdleTimeoutTask(ChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        protected void run(ChannelHandlerContext ctx) {

            final long currentLastWriteTime = MutableIdleStateHandler.this.lastWriteTime;
            final long nextDelay = writerIdleTimeNanos - (ticksInNanos() - currentLastWriteTime);
            if (nextDelay <= 0) {
                // Writer is idle - set a new timeout and notify the callback.
                writerIdleTimeout = schedule(ctx, this, writerIdleTimeNanos, TimeUnit.NANOSECONDS);

                final boolean first = firstWriterIdleEvent;
                firstWriterIdleEvent = false;

                try {
                    if (hasOutputChanged(ctx, first)) {
                        return;
                    }

                    final IdleStateEvent event = newIdleStateEvent(IdleState.WRITER_IDLE, first);
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                // Write occurred before the timeout - set a new timeout with shorter delay.
                writerIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }

    private final class AllIdleTimeoutTask extends MutableIdleStateHandler.AbstractIdleTask {

        AllIdleTimeoutTask(ChannelHandlerContext ctx) {
            super(ctx);
        }

        @Override
        protected void run(ChannelHandlerContext ctx) {

            long nextDelay = allIdleTimeNanos;
            if (!reading) {
                nextDelay -= ticksInNanos() - Math.max(lastReadTime, lastWriteTime);
            }
            if (nextDelay <= 0) {
                // Both reader and writer are idle - set a new timeout and
                // notify the callback.
                allIdleTimeout = schedule(ctx, this, allIdleTimeNanos, TimeUnit.NANOSECONDS);

                final boolean first = firstAllIdleEvent;
                firstAllIdleEvent = false;

                try {
                    if (hasOutputChanged(ctx, first)) {
                        return;
                    }

                    final IdleStateEvent event = newIdleStateEvent(IdleState.ALL_IDLE, first);
                    channelIdle(ctx, event);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                // Either read or write occurred before the timeout - set a new
                // timeout with shorter delay.
                allIdleTimeout = schedule(ctx, this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }
}
