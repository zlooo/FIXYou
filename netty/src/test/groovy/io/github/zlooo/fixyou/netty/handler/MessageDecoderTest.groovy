package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
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
        encodedMessage.refCnt() == 1 //should be increased when buffer is set on fix message
        1 * fixMessageObjectPool.tryGetAndRetain() >> fixMessage
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        fixMessage.messageByteSource.is(encodedMessage)
        0 * _
    }

    def "should decode fragmented message"() {
        setup:
        ByteBuf encodedMessage = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        encodedMessage.refCnt() == 2
        1 * fixMessageObjectPool.tryGetAndRetain() >> fixMessage
        messageDecoder.@state == MessageDecoder.State.DECODING
        fixMessage.messageByteSource.is(encodedMessage)
        0 * _
    }

    def "should finish off decoding fragmented message"() {
        setup:
        ByteBuf encodedMessagePart1 = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))
        fixMessageObjectPool.tryGetAndRetain() >> fixMessage
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart1)
        ByteBuf encodedMessagePart2 = Unpooled.wrappedBuffer("st\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart2)

        then:
        encodedMessagePart1.refCnt() == 1
        encodedMessagePart2.refCnt() == 1
        !fixMessage.messageByteSource.is(encodedMessagePart1)
        !fixMessage.messageByteSource.is(encodedMessagePart2)
        fixMessage.messageByteSource.refCnt() == 1
        fixMessage.messageByteSource.readerIndex(0).toString(StandardCharsets.US_ASCII) == "FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u0001"
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value.toString() == "test"
        0 * _
    }

    def "should decode 2 fragmented messages"() {
        //I know this is not 100% unit test and the fact that I'm using then block twice is quite a significant hint for that. I just want to check in an easy way if component
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
        1 * fixMessageObjectPool.tryGetAndRetain() >> fixMessage
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        !fixMessage.messageByteSource.is(encodedMessage1Part1)
        !fixMessage.messageByteSource.is(encodedMessage1Part2)
        encodedMessage1Part1.refCnt() == 1
        encodedMessage1Part2.refCnt() == 1
        fixMessage.messageByteSource.refCnt() == 1
        fixMessage.messageByteSource.readerIndex(0).toString(StandardCharsets.US_ASCII) == "FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u0001"
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value.toString() == "test"

        fixMessage.resetAllDataFields()

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage2Part1)
        messageDecoder.channelRead(channelHandlerContext, encodedMessage2Part2)

        then:
        encodedMessage1Part1.refCnt() == 0
        encodedMessage1Part2.refCnt() == 0
        encodedMessage2Part1.refCnt() == 1
        encodedMessage2Part2.refCnt() == 1
        1 * fixMessageObjectPool.tryGetAndRetain() >> fixMessage
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        !fixMessage.messageByteSource.is(encodedMessage1Part1)
        !fixMessage.messageByteSource.is(encodedMessage1Part2)
        !fixMessage.messageByteSource.is(encodedMessage2Part1)
        !fixMessage.messageByteSource.is(encodedMessage2Part2)
        fixMessage.messageByteSource.refCnt() == 1
        fixMessage.messageByteSource.readerIndex(0).toString(StandardCharsets.US_ASCII) == "FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u0001"
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value.toString() == "test"
        0 * _
    }

    def "should reset state"() {
        setup:
        messageDecoder.@state = MessageDecoder.State.DECODING
        fixMessage.retain() //that's because it's going to be released when fixMessageParser is being reset
        messageDecoder.@fixMessageParser.@fixMessage = fixMessage

        when:
        messageDecoder.reset()

        then:
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        messageDecoder.@fixMessageParser.@fixMessage == null
    }

    def "should reset state when channel becomes active"() {
        setup:
        messageDecoder.@state = MessageDecoder.State.DECODING
        fixMessage.retain() //that's because it's going to be released when fixMessageParser is being reset
        messageDecoder.@fixMessageParser.@fixMessage = fixMessage

        when:
        messageDecoder.channelActive(channelHandlerContext)

        then:
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        messageDecoder.@fixMessageParser.@fixMessage == null
        1 * channelHandlerContext.fireChannelActive()
        0 * _
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
        FixMessage fixMessage2 = new FixMessage(TestSpec.INSTANCE)

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        2 * fixMessageObjectPool.tryGetAndRetain() >> fixMessage >> fixMessage2
        1 * channelHandlerContext.fireChannelRead(fixMessage) >> channelHandlerContext
        1 * channelHandlerContext.fireChannelRead(fixMessage2) >> channelHandlerContext
        fixMessage.getField(58).value.toString() == "test"
        fixMessage2.getField(58).value.toString() == "test2"
        fixMessage.messageByteSource.readerIndex(0).toString(StandardCharsets.US_ASCII) ==
        "8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u00018=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test2\u000110=023\u0001"
        fixMessage2.messageByteSource.readerIndex(0).toString(StandardCharsets.US_ASCII) ==
        "8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u00018=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test2\u000110=023\u0001"
        encodedMessage.refCnt() == 2
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        0 * _
    }

    def "should decode multiple messages batched together in 2 packets"() {
        setup:
        ByteBuf encodedMessagePart1 = Unpooled.
                wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u00018=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=tes".getBytes(StandardCharsets.US_ASCII))
        ByteBuf encodedMessagePart2 = Unpooled.wrappedBuffer("t2\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))
        FixMessage fixMessage2 = new FixMessage(TestSpec.INSTANCE)

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart1)
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart2)

        then:
        2 * fixMessageObjectPool.tryGetAndRetain() >> fixMessage >> fixMessage2
        1 * channelHandlerContext.fireChannelRead(fixMessage) >> channelHandlerContext
        1 * channelHandlerContext.fireChannelRead(fixMessage2) >> channelHandlerContext
        fixMessage.getField(58).value.toString() == "test"
        fixMessage2.getField(58).value.toString() == "test2"
        fixMessage.messageByteSource.readerIndex(0).toString(StandardCharsets.US_ASCII) == "8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u00018=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=tes"
        fixMessage.messageByteSource.is(encodedMessagePart1)
        fixMessage2.messageByteSource != encodedMessagePart1
        fixMessage2.messageByteSource != encodedMessagePart2
        fixMessage2.messageByteSource.readerIndex(0).toString(StandardCharsets.US_ASCII) == "FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test2\u000110=023\u0001"
        fixMessage2.messageByteSource.refCnt() == 1
        encodedMessagePart1.refCnt() == 2 //fixMessage still holds 1 reference
        encodedMessagePart2.refCnt() == 1
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        0 * _
    }

    def "should release underlying buffers after decoding multiple messages batched together in 2 packets"() {
        setup:
        ByteBuf encodedMessagePart1 = Unpooled.
                wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u00018=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=tes".getBytes(StandardCharsets.US_ASCII))
        ByteBuf encodedMessagePart2 = Unpooled.wrappedBuffer("t2\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))
        FixMessage fixMessage2 = new FixMessage(TestSpec.INSTANCE)
        fixMessage.retain()
        fixMessage2.retain()

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart1)
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart2)
        def messageByteSource = fixMessage.messageByteSource
        fixMessage.release()
        def message2ByteSource = fixMessage2.messageByteSource
        fixMessage2.release()

        then:
        2 * fixMessageObjectPool.tryGetAndRetain() >> fixMessage >> fixMessage2
        1 * channelHandlerContext.fireChannelRead(fixMessage) >> channelHandlerContext
        1 * channelHandlerContext.fireChannelRead(fixMessage2) >> channelHandlerContext
        fixMessage.messageByteSource == null
        messageByteSource.refCnt() == 0
        fixMessage2.messageByteSource == null
        message2ByteSource.refCnt() == 0
        encodedMessagePart1.refCnt() == 0
        encodedMessagePart2.refCnt() == 0
    }
}
