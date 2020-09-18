package io.github.zlooo.fixyou.netty.handler;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.commons.NamingThreadFactory;
import io.github.zlooo.fixyou.fix.commons.FixMessageListener;
import io.github.zlooo.fixyou.netty.AbstractNettyAwareFixMessageListener;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.session.SessionID;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
@ChannelHandler.Sharable
class FixMessageListenerInvokingHandler extends SimpleChannelInboundHandler<FixMessage> implements Closeable {

    private final FixMessageListener fixMessageListener;
    private final boolean invokeDirectly;
    private final Disruptor<Event> disruptor;

    FixMessageListenerInvokingHandler(FixMessageListener fixMessageListener, FIXYouConfiguration configuration) {
        super(!configuration.isSeparateIoFromAppThread());
        this.fixMessageListener = fixMessageListener;
        if (configuration.isSeparateIoFromAppThread()) {
            disruptor = new Disruptor<>(() -> new Event(fixMessageListener), configuration.getFixMessageListenerInvokerDisruptorSize(), new NamingThreadFactory("FixMessageListenerInvoker"), ProducerType.MULTI,
                                        PhasedBackoffWaitStrategy.withSleep(DefaultConfiguration.FIX_MESSAGE_LISTENER_INVOKER_DISRUPTOR_TIMEOUT, DefaultConfiguration.FIX_MESSAGE_LISTENER_INVOKER_DISRUPTOR_TIMEOUT,
                                                                            TimeUnit.MILLISECONDS));
            for (int i = 0; i < configuration.getNumberOfAppThreads(); i++) {
                disruptor.handleEventsWith(new FixMessageListenerEventHandler(i));
            }
            disruptor.start();
            invokeDirectly = false;
        } else {
            disruptor = null;
            invokeDirectly = true;
        }
    }

    @Override
    public void close() {
        if (disruptor != null) {
            disruptor.shutdown();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (fixMessageListener instanceof AbstractNettyAwareFixMessageListener) {
            ((AbstractNettyAwareFixMessageListener) fixMessageListener).setChannel(ctx.channel());
        }
        ctx.fireChannelActive();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FixMessage msg) throws Exception {
        if (invokeDirectly) {
            fixMessageListener.onFixMessage(NettyHandlerAwareSessionState.getSessionID(ctx), msg);
        } else {
            try {
                final RingBuffer<Event> ringBuffer = disruptor.getRingBuffer();
                final long sequence = ringBuffer.tryNext();
                final Event event = ringBuffer.get(sequence);
                final Channel channel = ctx.channel();
                event.sessionID = channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY).get().getSessionId();
                event.eventHandlerNumber = channel.attr(FIXYouChannelInitializer.ORDINAL_NUMBER_KEY).get();
                event.fixMessage = msg;
                ringBuffer.publish(sequence);
            } catch (InsufficientCapacityException e) {
                log.warn("Insufficient capacity in disruptor's ring buffer, have to drop one or else boom goes the dynamite");
            }
        }
    }

    @RequiredArgsConstructor
    private static class Event {
        private final FixMessageListener fixMessageListener;
        private SessionID sessionID;
        private FixMessage fixMessage;
        private int eventHandlerNumber;
    }

    @RequiredArgsConstructor
    private static class FixMessageListenerEventHandler implements EventHandler<Event> {

        private final int eventHandlerNumber;

        @Override
        public void onEvent(Event event, long sequence, boolean endOfBatch) throws Exception {
            if (event.eventHandlerNumber == eventHandlerNumber) {
                try {
                    event.fixMessageListener.onFixMessage(event.sessionID, event.fixMessage);
                } finally {
                    event.fixMessage.release();
                }
            }
        }
    }
}
