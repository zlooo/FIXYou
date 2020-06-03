package io.github.zlooo.fixyou.netty.handler;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.commons.utils.ReflectionUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.util.concurrent.ImmediateEventExecutor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@ChannelHandler.Sharable
class AsyncExecutingHandler implements ChannelInboundHandler, Closeable {

    private static final int RING_BUFFER_SIZE = 2048;
    private static final int WAIT_METHOD_TIMEOUT = 100;
    private static final LoggingExceptionHandler EXCEPTION_HANDLER = new LoggingExceptionHandler();
    private static final EventHandler<MessageStateHolder> NO_ORDINAL_NUMBER_EVENT_HANDLER = (event, sequence, endOfBatch) -> event.channelHandlerContext.fireChannelRead(event.message);
    private final Disruptor<MessageStateHolder> disruptor;

    @Inject
    AsyncExecutingHandler(FIXYouConfiguration fixYouConfiguration) {
        this.disruptor =
                new Disruptor<>(MessageStateHolder::new, RING_BUFFER_SIZE, Executors.defaultThreadFactory(), ProducerType.SINGLE, PhasedBackoffWaitStrategy.withSleep(WAIT_METHOD_TIMEOUT, WAIT_METHOD_TIMEOUT, TimeUnit.MILLISECONDS));
        final int numberOfAppThreads = fixYouConfiguration.getNumberOfAppThreads();
        if (numberOfAppThreads == 1) {
            disruptor.handleEventsWith(NO_ORDINAL_NUMBER_EVENT_HANDLER);
        } else {
            for (int i = 0; i < numberOfAppThreads; i++) {
                disruptor.handleEventsWith(new OrdinalNumberCheckingEventHandler(i));
            }
        }
        disruptor.setDefaultExceptionHandler(EXCEPTION_HANDLER);
        disruptor.start();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        setExecutorOnDownstreamChannels(ctx, ImmediateEventExecutor.INSTANCE);
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        setExecutorOnDownstreamChannels(ctx, null);
        ctx.fireChannelUnregistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            disruptor.publishEvent((event, sequence, channelChandlerContext, message) -> {
                event.channelHandlerContext = channelChandlerContext;
                event.message = message;
                event.ordinalNumber = ctx.channel().id()
            }, ctx, (ByteBuf) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

    }

    @Override
    public void close() {
        disruptor.shutdown();
    }

    //TODO that's quite an unsexy and even evil method, I should refactor this mechanism so that I do not have to use reflection here
    private static void setExecutorOnDownstreamChannels(ChannelHandlerContext ctx, ImmediateEventExecutor eventExecutor) {
        ChannelHandlerContext currentContext = ctx;
        while ((currentContext = ReflectionUtils.getFieldValue(currentContext, "next", ChannelHandlerContext.class)) != null) {
            ReflectionUtils.setFinalField(currentContext, "executor", eventExecutor);
        }
    }

    @Data
    private static class MessageStateHolder {
        private ByteBuf message;
        private ChannelHandlerContext channelHandlerContext;
        private int ordinalNumber;
    }

    @RequiredArgsConstructor
    private static class OrdinalNumberCheckingEventHandler implements EventHandler<MessageStateHolder> {

        private final int ordinalNumberHandled;

        @Override
        public void onEvent(MessageStateHolder event, long sequence, boolean endOfBatch) throws Exception {
            if (event.ordinalNumber == ordinalNumberHandled) {
                NO_ORDINAL_NUMBER_EVENT_HANDLER.onEvent(event, sequence, endOfBatch);
            }
        }
    }

    private static class LoggingExceptionHandler implements ExceptionHandler<MessageStateHolder> {
        @Override
        public void handleEventException(Throwable ex, long sequence, MessageStateHolder event) {
            log.error("Exception when processing event {}", event, ex);
        }

        @Override
        public void handleOnStartException(Throwable ex) {
            log.error("Exception when starting disruptor", ex);
        }

        @Override
        public void handleOnShutdownException(Throwable ex) {
            log.error("Exception when stopping disruptor", ex);
        }
    }
}
