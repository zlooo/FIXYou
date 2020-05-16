package pl.zlooo.fixyou.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.handler.flush.FlushConsolidationHandler;
import pl.zlooo.fixyou.Resettable;

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
