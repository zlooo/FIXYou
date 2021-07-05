package io.github.zlooo.fixyou.netty.handler.validation;

import io.github.zlooo.fixyou.commons.AbstractPoolableFixMessage;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.model.FixMessage;
import io.netty.channel.ChannelHandlerContext;

@FunctionalInterface
public interface ValidationFailureAction {

    void perform(ChannelHandlerContext channelHandlerContext, FixMessage fixMessage, ObjectPool<? extends AbstractPoolableFixMessage> fixMessageObjectPool);
}
