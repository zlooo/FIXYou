package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.netty.utils.ValueAddingByteProcessor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;

@ChannelHandler.Sharable
class StatefulMessageEncoder extends AbstractMessageEncoder implements Resettable, Closeable {

    private final ByteBuf bodyTempBuffer = Unpooled.directBuffer(DefaultConfiguration.AVG_FIELDS_PER_MESSAGE * DefaultConfiguration.FIELD_BUFFER_SIZE);
    private final ValueAddingByteProcessor valueAddingByteProcessor = new ValueAddingByteProcessor();

    @Override
    public void reset() {
        bodyTempBuffer.clear();
        valueAddingByteProcessor.reset();
    }

    @Override
    protected ValueAddingByteProcessor getValueAddingByteProcessor() {
        return valueAddingByteProcessor;
    }

    @Override
    protected ByteBuf getBodyTempBuffer() {
        return bodyTempBuffer.clear();
    }

    @Override
    public void close() {
        final int bufferRefCount = bodyTempBuffer.refCnt();
        if (bufferRefCount > 0) {
            bodyTempBuffer.release(bufferRefCount);
        }
    }
}
