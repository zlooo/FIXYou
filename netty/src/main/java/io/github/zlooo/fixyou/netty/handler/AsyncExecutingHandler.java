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
    private static final EventHandler<MessageStateHolder> NO_ORDINAL_NUMBER_EVENT_HANDLER = eventHandler();

    private final Disruptor<MessageStateHolder> disruptor;

    private static enum CtxMethod {
        READ, READ_COMPLETE, USER_EVENT, WRITABILITY_CHANGED, EXCEPTION_CAUGHT
    }

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
        ctx.fireChannelUnregistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        setExecutorOnDownstreamChannels(ctx, null);
        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            publishEventOnDisruptor(ctx, msg, CtxMethod.READ);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        publishEventOnDisruptor(ctx, null, CtxMethod.READ_COMPLETE);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        publishEventOnDisruptor(ctx, evt, CtxMethod.USER_EVENT);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        publishEventOnDisruptor(ctx, null, CtxMethod.WRITABILITY_CHANGED);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        //nothing to do
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        //nothing to do
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        publishEventOnDisruptor(ctx, cause, CtxMethod.EXCEPTION_CAUGHT);
    }

    @Override
    public void close() {
        disruptor.shutdown();
    }

    private void publishEventOnDisruptor(ChannelHandlerContext ctx, Object secondArg, CtxMethod ctxMethod) {
        disruptor.publishEvent((event, sequence, channelChandlerContext, secondArgument, contextMethod) -> {
            event.channelHandlerContext = channelChandlerContext;
            event.secondArg = secondArgument;
            event.ordinalNumber = ctx.channel().attr(FIXYouChannelInitializer.ORDINAL_NUMBER_KEY).get();
            event.ctxMethod = contextMethod;
        }, ctx, secondArg, ctxMethod);
    }

    //TODO that's quite an unsexy and even evil method, I should refactor this mechanism so that I do not have to use reflection here
    private static void setExecutorOnDownstreamChannels(ChannelHandlerContext ctx, ImmediateEventExecutor eventExecutor) {
        ChannelHandlerContext currentContext = ctx;
        while ((currentContext = ReflectionUtils.getFieldValue(currentContext, "next", ChannelHandlerContext.class)) != null) {
            ReflectionUtils.setFinalField(currentContext, "executor", eventExecutor);
        }
    }

    private static EventHandler<MessageStateHolder> eventHandler() {
        return (event, sequence, endOfBatch) -> {
            switch (event.ctxMethod) {
                case READ:
                    event.channelHandlerContext.fireChannelRead(event.secondArg);
                    break;
                case READ_COMPLETE:
                    event.channelHandlerContext.fireChannelReadComplete();
                    break;
                case WRITABILITY_CHANGED:
                    event.channelHandlerContext.fireChannelWritabilityChanged();
                    break;
                case USER_EVENT:
                    event.channelHandlerContext.fireUserEventTriggered(event.secondArg);
                    break;
                case EXCEPTION_CAUGHT:
                    event.channelHandlerContext.fireExceptionCaught((Throwable) event.secondArg);
                    break;
                default:
                    throw new IllegalArgumentException("Unrecognized value of CtxMethod " + event.ctxMethod);
            }
        };
    }

    @Data
    private static class MessageStateHolder {
        private Object secondArg;
        private ChannelHandlerContext channelHandlerContext;
        private int ordinalNumber;
        private CtxMethod ctxMethod;
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
