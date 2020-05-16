package pl.zlooo.fixyou.netty.handler

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool
import pl.zlooo.fixyou.netty.handler.admin.TestSpec
import pl.zlooo.fixyou.parser.model.FixMessage
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class MessageDecoderTest extends Specification {

    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock()
    private MessageDecoder messageDecoder = new MessageDecoder(fixMessageObjectPool)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)

    def "should decode not fragmented message"() {
        setup:
        ByteBuf encodedMessage = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
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
        1 * fixMessageObjectPool.getAndRetain() >> fixMessage
        messageDecoder.@state == MessageDecoder.State.DECODING
        0 * _
    }

    def "should finish off decoding fragmented message"() {
        setup:
        ByteBuf encodedMessagePart1 = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))
        messageDecoder.@state = MessageDecoder.State.DECODING
        messageDecoder.@fixMessageReader.setFixBytes(encodedMessagePart1)
        messageDecoder.@fixMessageReader.setFixMessage(fixMessage)
        messageDecoder.@fixMessageReader.parseFixMsgBytes()
        ByteBuf encodedMessagePart2 = Unpooled.wrappedBuffer("st\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart2)

        then:
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value == "test".toCharArray()
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
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value == "test".toCharArray()

        fixMessage.resetAllDataFields()

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage2Part1)
        messageDecoder.channelRead(channelHandlerContext, encodedMessage2Part2)

        then:
        1 * fixMessageObjectPool.getAndRetain() >> fixMessage
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value == "test".toCharArray()
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
        2 * fixMessageObjectPool.getAndRetain() >> fixMessage
        1 * channelHandlerContext.fireChannelRead({ it.getField(58).value == "test".toCharArray() }) >> {FixMessage msg -> msg.resetAllDataFields(); return channelHandlerContext}
        1 * channelHandlerContext.fireChannelRead({ it.getField(58).value == "test2".toCharArray() }) >> {FixMessage msg -> msg.resetAllDataFields(); return channelHandlerContext}
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        0 * _
    }
}
