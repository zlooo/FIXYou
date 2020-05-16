package pl.zlooo.fixyou.netty.handler.validation;

import io.netty.channel.ChannelHandlerContext;
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool;
import pl.zlooo.fixyou.parser.model.FixMessage;

@FunctionalInterface
public interface ValidationFailureAction {

    void perform(ChannelHandlerContext channelHandlerContext, FixMessage fixMessage, DefaultObjectPool<FixMessage> fixMessageObjectPool);
}
