package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class MessageDecoderTest extends Specification {

    private DefaultObjectPool<io.github.zlooo.fixyou.parser.model.FixMessage> fixMessageObjectPool = Mock()
    private MessageDecoder messageDecoder = new MessageDecoder(fixMessageObjectPool)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private io.github.zlooo.fixyou.parser.model.FixMessage fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(TestSpec.INSTANCE)

    def "should decode not fragmented message"() {
        setup:
        ByteBuf encodedMessage = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        encodedMessage.refCnt() == 0
        1 * fixMessageObjectPool.getAndRetain() >> fixMessage
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        0 * _
    }

    def "should decode fragmented message"() {
        setup:
        ByteBuf encodedMessage = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        encodedMessage.refCnt() == 1
        1 * fixMessageObjectPool.getAndRetain() >> fixMessage
        messageDecoder.@state == MessageDecoder.State.DECODING
        0 * _
    }

    def "should finish off decoding fragmented message"() {
        setup:
        ByteBuf encodedMessagePart1 = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))
        fixMessageObjectPool.getAndRetain() >> fixMessage
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart1)
        ByteBuf encodedMessagePart2 = Unpooled.wrappedBuffer("st\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart2)

        then:
        encodedMessagePart1.refCnt() == 0
        encodedMessagePart2.refCnt() == 0
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TEXT_FIELD_NUMBER).value == "test".toCharArray()
        0 * _
    }

    def "should decode 2 fragmented messages"() {
        //I know this is not 100% unit test and the fact that I'm using then block twice is quite a significant fint for that. I just want to check in an easy way if component
        // reset it's state properly
        setup:
        ByteBuf encodedMessage1Part1 = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))
        ByteBuf encodedMessage1Part2 = Unpooled.wrappedBuffer("st\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))
        ByteBuf encodedMessage2Part1 = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))
        ByteBuf encodedMessage2Part2 = Unpooled.wrappedBuffer("st\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage1Part1)
        messageDecoder.channelRead(channelHandlerContext, encodedMessage1Part2)

        then:
        1 * fixMessageObjectPool.getAndRetain() >> fixMessage
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TEXT_FIELD_NUMBER).value == "test".toCharArray()

        fixMessage.resetAllDataFields()

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage2Part1)
        messageDecoder.channelRead(channelHandlerContext, encodedMessage2Part2)

        then:
        encodedMessage1Part1.refCnt() == 0
        encodedMessage1Part2.refCnt() == 0
        encodedMessage2Part1.refCnt() == 0
        encodedMessage2Part2.refCnt() == 0
        1 * fixMessageObjectPool.getAndRetain() >> fixMessage
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TEXT_FIELD_NUMBER).value == "test".toCharArray()
        0 * _
    }

    def "should reset state"() {
        setup:
        messageDecoder.@state = MessageDecoder.State.DECODING

        when:
        messageDecoder.reset()

        then:
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
    }

    def "should pass further message that's not ByteBuf"() {
        setup:
        Object message = new Object()

        when:
        messageDecoder.channelRead(channelHandlerContext, message)

        then:
        1 * channelHandlerContext.fireChannelRead(message)
        0 * _
    }

    def "should decode multiple messages batched together"() {
        setup:
        ByteBuf encodedMessage = Unpooled.
                wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u00018=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test2\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        encodedMessage.refCnt() == 0
        2 * fixMessageObjectPool.getAndRetain() >> fixMessage
        1 * channelHandlerContext.fireChannelRead({ it.getField(58).value == "test".toCharArray() }) >> { io.github.zlooo.fixyou.parser.model.FixMessage msg -> msg.resetAllDataFields(); return channelHandlerContext }
        1 * channelHandlerContext.fireChannelRead({ it.getField(58).value == "test2".toCharArray() }) >> { io.github.zlooo.fixyou.parser.model.FixMessage msg -> msg.resetAllDataFields(); return channelHandlerContext }
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        0 * _
    }
}
