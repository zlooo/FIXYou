package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.netty.utils.ValueAddingByteProcessor;
import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
class StatefulMessageEncoder extends AbstractMessageEncoder implements Resettable {

    private final ValueAddingByteProcessor valueAddingByteProcessor = new ValueAddingByteProcessor();

    @Override
    public void reset() {
        valueAddingByteProcessor.reset();
    }

    @Override
    protected ValueAddingByteProcessor getValueAddingByteProcessor() {
        return valueAddingByteProcessor;
    }
}
