package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.EventLoop
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class ReadTaskTest extends Specification {
    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock()
    private MessageDecoder messageDecoder = new MessageDecoder(fixMessageObjectPool)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private EventLoop eventLoop = Mock()
    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)

    void setup() {
        messageDecoder.handlerAdded(channelHandlerContext)
        messageDecoder.readTask.taskScheduled=true
    }

    def "should reschedule task if there is still some data to parse"() {
        setup:
        messageDecoder.byteBufComposer.addByteBuf(Unpooled.wrappedBuffer("something".getBytes(StandardCharsets.US_ASCII)))

        when:
        messageDecoder.readTask.run()

        then:
        1 * fixMessageObjectPool.tryGetAndRetain() >> null
        messageDecoder.readTask.taskScheduled
        1 * channelHandlerContext.channel() >> channel
        1 * channel.eventLoop() >> eventLoop
        1 * eventLoop.execute(messageDecoder.readTask)
        0 * _
    }

    def "should not reschedule task if there is no data left to parse"() {
        setup:
        messageDecoder.byteBufComposer.addByteBuf(Unpooled.wrappedBuffer("something".getBytes(StandardCharsets.US_ASCII)))
        messageDecoder.byteBufComposer.readerIndex(9)

        when:
        messageDecoder.readTask.run()

        then:
        !messageDecoder.readTask.taskScheduled
        0 * _
    }
}
