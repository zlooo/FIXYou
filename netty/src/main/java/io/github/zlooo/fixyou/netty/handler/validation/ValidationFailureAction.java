package io.github.zlooo.fixyou.netty.handler.validation;

import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.channel.ChannelHandlerContext;

@FunctionalInterface
public interface ValidationFailureAction {

    void perform(ChannelHandlerContext channelHandlerContext, FixMessage fixMessage, ObjectPool<FixMessage> fixMessageObjectPool);
}
