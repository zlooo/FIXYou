package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.Resettable;
import io.netty.channel.ChannelHandler;
import io.netty.handler.flush.FlushConsolidationHandler;

@ChannelHandler.Sharable
class ResettableFlushConsolidationHandler extends FlushConsolidationHandler implements Resettable {

    ResettableFlushConsolidationHandler() {
        super(FlushConsolidationHandler.DEFAULT_EXPLICIT_FLUSH_AFTER_FLUSHES, true);
    }

    @Override
    public void reset() {
        //nothing to do
    }
}
