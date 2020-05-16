package pl.zlooo.fixyou.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import pl.zlooo.fixyou.FIXYouConfiguration;
import pl.zlooo.fixyou.Resettable;
import pl.zlooo.fixyou.fix.commons.config.validator.ConfigValidator;
import pl.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import pl.zlooo.fixyou.netty.handler.FixYouNettyComponent;
import pl.zlooo.fixyou.netty.handler.Handlers;
import pl.zlooo.fixyou.netty.handler.NettyResettablesNames;
import pl.zlooo.fixyou.netty.utils.PipelineUtils;
import pl.zlooo.fixyou.parser.model.FixMessage;
import pl.zlooo.fixyou.session.SessionConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class FIXYouNettyInitiator extends AbstractFIXYouNetty {

    private final Bootstrap bootstrap;

    FIXYouNettyInitiator(FixYouNettyComponent fixYouNettyComponent, FIXYouConfiguration fixYouConfiguration, ConfigValidator configValidator) {
        super(fixYouNettyComponent, fixYouConfiguration, configValidator);
        this.bootstrap = new Bootstrap().channel(NioSocketChannel.class).handler(fixYouNettyComponent.channelInitializer()).group(eventLoopGroup);
    }

    @Override
    public Future<Void> start() {
        final MultipleVoidFuturesWrapper futuresWrapper = new MultipleVoidFuturesWrapper();
        for (final NettyHandlerAwareSessionState sessionState : (Collection<NettyHandlerAwareSessionState>) fixYouNettyComponent.sessionRegistry().getAll()) {
            futuresWrapper.futures.add(connectSession(sessionState));
        }
        return futuresWrapper;
    }

    private Future<Void> connectSession(NettyHandlerAwareSessionState sessionState) {
        final SessionConfig sessionConfig = sessionState.getSessionConfig();
        final ChannelFuture connectFuture = bootstrap.connect(sessionConfig.getHost(), sessionConfig.getPort()).addListener(
                (ChannelFutureListener) future -> {
                    final Channel channel = future.channel();
                    final long heartbeatInterval = sessionConfig.getHeartbeatInterval();
                    PipelineUtils.addRequiredHandlersToPipeline(channel, sessionState, fixYouNettyComponent.beforeSessionMessageValidatorHandler(), fixYouNettyComponent.afterSessionMessageValidatorHandler(), heartbeatInterval,
                                                                Handlers.SESSION);
                    final FixMessage logonMessage =
                            FixMessageUtils.toLogonMessage(sessionState.getFixMessageObjectPool().getAndRetain(), sessionState.getFixSpec().applicationVersionId().getValue(), sessionConfig.getEncryptMethod(), heartbeatInterval, false);
                    final Map<String, Resettable> resettables = sessionState.getResettables();
                    final ChannelOutboundHandler sessionHandler = (ChannelOutboundHandler) resettables.get(NettyResettablesNames.SESSION);
                    sessionHandler.write((ChannelHandlerContext) resettables.get(NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT), logonMessage, null);
                    channel.writeAndFlush(logonMessage).addListener(logonSendFuture -> {
                        if (logonSendFuture.isSuccess()) {
                            sessionState.setLogonSent(true);
                        }
                    }).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                });
        connectFuture.channel().closeFuture().addListener(future -> eventLoopGroup.schedule(this::start, fixYouConfiguration.getReconnectIntervalMillis(), TimeUnit.MILLISECONDS));
        return connectFuture;
    }

    private static final class MultipleVoidFuturesWrapper implements Future<Void> {

        private List<Future<Void>> futures = new ArrayList<>();

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean result = true;
            for (final Future<Void> future : futures) {
                result &= future.cancel(mayInterruptIfRunning);
            }
            return result;
        }

        @Override
        public boolean isCancelled() {
            boolean result = true;
            for (final Future<Void> future : futures) {
                result &= future.isCancelled();
            }
            return result;
        }

        @Override
        public boolean isDone() {
            boolean result = true;
            for (final Future<Void> future : futures) {
                result &= future.isDone();
            }
            return result;
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            while (!isDone()) {
                //wait till it's done
            }
            return null;
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            final long currentTimeMillis = System.currentTimeMillis();
            final long waitUntil = currentTimeMillis + unit.toMillis(timeout);
            while (!isDone()) {
                if (System.currentTimeMillis() > waitUntil) {
                    throw new TimeoutException();
                }
            }
            return null;
        }
    }
}
