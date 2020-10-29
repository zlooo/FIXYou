package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.netty.utils.DelegatingChannelHandlerContext;
import io.github.zlooo.fixyou.parser.model.FieldCodec;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.session.SessionConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Singleton
public class NettyResettablesSupplier implements Function<NettyHandlerAwareSessionState, Map<String, Resettable>> {

    private final FieldCodec fieldCodec;
    private final ObjectPool<FixMessage> fixMessageObjectPool;

    @Inject
    NettyResettablesSupplier(FieldCodec fieldCodec, @Named("fixMessageObjectPool") ObjectPool fixMessageObjectPool) {
        this.fieldCodec = fieldCodec;
        this.fixMessageObjectPool = fixMessageObjectPool;
    }

    @Override
    public Map<String, Resettable> apply(NettyHandlerAwareSessionState nettyHandlerAwareSessionState) {
        final Map<String, Resettable> resetables = new HashMap<>();
        resetables.put(NettyResettablesNames.SESSION, new SessionHandler(nettyHandlerAwareSessionState, fixMessageObjectPool));
        resetables.put(NettyResettablesNames.MESSAGE_DECODER, new MessageDecoder(nettyHandlerAwareSessionState.getFixSpec(), fieldCodec));
        resetables.put(NettyResettablesNames.IDLE_STATE_HANDLER, new MutableIdleStateHandler(nettyHandlerAwareSessionState, fixMessageObjectPool));
        final SessionConfig sessionConfig = nettyHandlerAwareSessionState.getSessionConfig();
        if (sessionConfig.isPersistent() && sessionConfig.getMessageStore() != null) {
            resetables.put(NettyResettablesNames.MESSAGE_STORE_HANDLER, new MessageStoreHandler(nettyHandlerAwareSessionState.getSessionId(), sessionConfig.getMessageStore()));
        }
        resetables.put(NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT,
                       new DelegatingChannelHandlerContext() {
                           @Override
                           public ChannelHandlerContext fireChannelRead(Object msg) {
                               //we don't want to move forward here
                               return this;
                           }

                           @Override
                           public ChannelFuture write(Object msg, ChannelPromise promise) {
                               //we don't want to move forward here
                               return promise;
                           }
                       });
        if (sessionConfig.isConsolidateFlushes()) {
            resetables.put(NettyResettablesNames.FLUSH_CONSOLIDATION_HANDLER, new ResettableFlushConsolidationHandler());
        }
        return Collections.unmodifiableMap(resetables);
    }
}
