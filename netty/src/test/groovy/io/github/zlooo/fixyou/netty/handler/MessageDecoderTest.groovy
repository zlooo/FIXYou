package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.commons.memory.Region
import io.github.zlooo.fixyou.commons.pool.ObjectPool
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.github.zlooo.fixyou.utils.UnsafeAccessor
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import org.assertj.core.api.Assertions
import org.assertj.core.data.Index
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class MessageDecoderTest extends Specification {

    private static Region region
    private ChannelHandlerContext channelHandlerContext = Mock()
    private ObjectPool objectPool = Mock {
        1 * tryGetAndRetain() >> region
    }
    private MessageDecoder messageDecoder = new MessageDecoder(TestSpec.INSTANCE, objectPool)
    private FixMessage fixMessage = messageDecoder.@fixMessage

    def setupSpec() {
        region = new Region(UnsafeAccessor.UNSAFE.allocateMemory(256), 256 as short)
    }

    def cleanupSpec() {
        UnsafeAccessor.UNSAFE.freeMemory(region.getStartingAddress())
    }

    def setup() {
        region.reset()
    }

    def "should decode not fragmented message"() {
        setup:
        ByteBuf encodedMessage = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=021\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        encodedMessage.refCnt() == 0
        1 * channelHandlerContext.fireChannelRead({
            verifyAll(it, FixMessage) { decodedFixMessage ->
                decodedFixMessage.is(fixMessage)
                decodedFixMessage.isValueSet(FixConstants.BEGIN_STRING_FIELD_NUMBER)
                decodedFixMessage.isValueSet(FixConstants.CHECK_SUM_FIELD_NUMBER)
                decodedFixMessage.getLongValue(FixConstants.CHECK_SUM_FIELD_NUMBER) == 21
            }
        })
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        messageDecoder.byteBufComposer.storedStartIndex == ByteBufComposer.INITIAL_VALUE
        messageDecoder.byteBufComposer.storedEndIndex == ByteBufComposer.INITIAL_VALUE
        Assertions.assertThat(messageDecoder.byteBufComposer.components).containsOnly(new ByteBufComposer.Component())
        0 * _
    }

    def "should decode fragmented message"() {
        setup:
        ByteBuf encodedMessage = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        encodedMessage.refCnt() == 1
        messageDecoder.@state == MessageDecoder.State.DECODING
        fixMessage.isValueSet(FixConstants.BEGIN_STRING_FIELD_NUMBER)
        fixMessage.isValueSet(56)
        fixMessage.getLongValue(9) == 28
        messageDecoder.byteBufComposer.storedStartIndex == 0
        messageDecoder.byteBufComposer.storedEndIndex == encodedMessage.writerIndex() - 1
        def expectedComponent = new ByteBufComposer.Component(startIndex: 0, endIndex: encodedMessage.writerIndex() - 1, buffer: encodedMessage)
        Assertions.assertThat(messageDecoder.byteBufComposer.components)
                  .containsOnlyOnce(expectedComponent)
                  .contains(expectedComponent, Index.atIndex(0))
                  .containsOnly(expectedComponent, new ByteBufComposer.Component())
        0 * _
    }

    def "should finish off decoding fragmented message"() {
        setup:
        ByteBuf encodedMessagePart1 = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158".getBytes(StandardCharsets.US_ASCII))
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart1)
        ByteBuf encodedMessagePart2 = Unpooled.wrappedBuffer("=test\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart2)

        then:
        encodedMessagePart1.refCnt() == 0
        encodedMessagePart2.refCnt() == 0
        1 * channelHandlerContext.fireChannelRead({
            verifyAll(it, FixMessage) { decodedFixMessage ->
                decodedFixMessage.is(fixMessage)
                decodedFixMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == "test"
                decodedFixMessage.isValueSet(FixConstants.BEGIN_STRING_FIELD_NUMBER)
                decodedFixMessage.isValueSet(FixConstants.CHECK_SUM_FIELD_NUMBER)
                decodedFixMessage.getLongValue(FixConstants.CHECK_SUM_FIELD_NUMBER) == 23
            }
        })
        messageDecoder.byteBufComposer.storedStartIndex == ByteBufComposer.INITIAL_VALUE
        messageDecoder.byteBufComposer.storedEndIndex == ByteBufComposer.INITIAL_VALUE
        Assertions.assertThat(messageDecoder.byteBufComposer.components).containsOnly(new ByteBufComposer.Component())
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        0 * _
    }

    def "should decode 2 fragmented messages"() {
        //I know this is not 100% unit test and the fact that I'm using then block twice is quite a significant hint for that. I just want to check in an easy way if component
        // reset it's state properly
        setup:
        ByteBuf encodedMessage1Part1 = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))
        ByteBuf encodedMessage1Part2 = Unpooled.wrappedBuffer("st\u000110=024\u0001".getBytes(StandardCharsets.US_ASCII))
        ByteBuf encodedMessage2Part1 = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=128\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))
        ByteBuf encodedMessage2Part2 = Unpooled.wrappedBuffer("st2\u000110=025\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage1Part1)
        messageDecoder.channelRead(channelHandlerContext, encodedMessage1Part2)

        then:
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        1 * channelHandlerContext.fireChannelRead({
            verifyAll(it, FixMessage) { decodedFixMessage ->
                decodedFixMessage.is(fixMessage)
                decodedFixMessage.isValueSet(FixConstants.BEGIN_STRING_FIELD_NUMBER)
                decodedFixMessage.isValueSet(FixConstants.CHECK_SUM_FIELD_NUMBER)
                decodedFixMessage.getLongValue(FixConstants.CHECK_SUM_FIELD_NUMBER) == 24
                decodedFixMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == "test"
            }
        })
        encodedMessage1Part1.refCnt() == 0
        encodedMessage1Part2.refCnt() == 0
        encodedMessage2Part1.refCnt() == 1
        encodedMessage2Part2.refCnt() == 1
        messageDecoder.byteBufComposer.storedStartIndex == ByteBufComposer.INITIAL_VALUE
        messageDecoder.byteBufComposer.storedEndIndex == ByteBufComposer.INITIAL_VALUE
        Assertions.assertThat(messageDecoder.byteBufComposer.components).containsOnly(new ByteBufComposer.Component())

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage2Part1)
        messageDecoder.channelRead(channelHandlerContext, encodedMessage2Part2)

        then:
        encodedMessage1Part1.refCnt() == 0
        encodedMessage1Part2.refCnt() == 0
        encodedMessage2Part1.refCnt() == 0
        encodedMessage2Part2.refCnt() == 0
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        1 * channelHandlerContext.fireChannelRead({
            verifyAll(it, FixMessage) { decodedFixMessage ->
                decodedFixMessage.is(fixMessage)
                decodedFixMessage.isValueSet(FixConstants.BEGIN_STRING_FIELD_NUMBER)
                decodedFixMessage.isValueSet(FixConstants.CHECK_SUM_FIELD_NUMBER)
                decodedFixMessage.getLongValue(FixConstants.CHECK_SUM_FIELD_NUMBER) == 25
                decodedFixMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == "test2"
            }
        })
        Assertions.assertThat(messageDecoder.byteBufComposer.components).containsOnly(new ByteBufComposer.Component())
        0 * _
    }

    def "should reset state"() {
        setup:
        messageDecoder.@state = MessageDecoder.State.DECODING
        fixMessage.retain() //that's because it's going to be released when fixMessageParser is being reset

        when:
        messageDecoder.reset()

        then:
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
    }

    def "should reset state when channel becomes active"() {
        setup:
        messageDecoder.@state = MessageDecoder.State.DECODING
        fixMessage.retain() //that's because it's going to be released when fixMessageParser is being reset

        when:
        messageDecoder.channelActive(channelHandlerContext)

        then:
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
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
                wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=026\u00018=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test2\u000110=027\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        1 * channelHandlerContext.fireChannelRead({
            verifyAll(it, FixMessage) { decodedMessage ->
                decodedMessage.is(fixMessage)
                decodedMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == "test"
                decodedMessage.isValueSet(FixConstants.BEGIN_STRING_FIELD_NUMBER)
                decodedMessage.isValueSet(FixConstants.CHECK_SUM_FIELD_NUMBER)
                decodedMessage.getLongValue(FixConstants.CHECK_SUM_FIELD_NUMBER) == 26
            }
        }) >> channelHandlerContext
        1 * channelHandlerContext.fireChannelRead({
            verifyAll(it, FixMessage) { decodedMessage ->
                decodedMessage.is(fixMessage)
                decodedMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == "test2"
                decodedMessage.isValueSet(FixConstants.BEGIN_STRING_FIELD_NUMBER)
                decodedMessage.isValueSet(FixConstants.CHECK_SUM_FIELD_NUMBER)
                decodedMessage.getLongValue(FixConstants.CHECK_SUM_FIELD_NUMBER) == 27
            }
        }) >> channelHandlerContext
        encodedMessage.refCnt() == 0
        messageDecoder.byteBufComposer.storedStartIndex == ByteBufComposer.INITIAL_VALUE
        messageDecoder.byteBufComposer.storedEndIndex == ByteBufComposer.INITIAL_VALUE
        Assertions.assertThat(messageDecoder.byteBufComposer.components).containsOnly(new ByteBufComposer.Component())
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        0 * _
    }

    def "should decode multiple messages batched together in 2 packets"() {
        setup:
        ByteBuf encodedMessagePart1 = Unpooled.
                wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=028\u00018=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=tes".getBytes(StandardCharsets.US_ASCII))
        ByteBuf encodedMessagePart2 = Unpooled.wrappedBuffer("t2\u000110=029\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart1)
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart2)

        then:
        1 * channelHandlerContext.fireChannelRead({
            verifyAll(it, FixMessage) { decodedMessage ->
                decodedMessage.is(fixMessage)
                decodedMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == "test"
                decodedMessage.isValueSet(FixConstants.BEGIN_STRING_FIELD_NUMBER)
                decodedMessage.isValueSet(FixConstants.CHECK_SUM_FIELD_NUMBER)
                decodedMessage.getLongValue(FixConstants.CHECK_SUM_FIELD_NUMBER) == 28
            }
        }) >> channelHandlerContext
        1 * channelHandlerContext.fireChannelRead({
            verifyAll(it, FixMessage) { decodedMessage ->
                decodedMessage.is(fixMessage)
                decodedMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == "test2"
                decodedMessage.isValueSet(FixConstants.BEGIN_STRING_FIELD_NUMBER)
                decodedMessage.isValueSet(FixConstants.CHECK_SUM_FIELD_NUMBER)
                decodedMessage.getLongValue(FixConstants.CHECK_SUM_FIELD_NUMBER) == 29
            }
        }) >> channelHandlerContext
        encodedMessagePart1.refCnt() == 0
        encodedMessagePart2.refCnt() == 0
        messageDecoder.byteBufComposer.storedStartIndex == ByteBufComposer.INITIAL_VALUE
        messageDecoder.byteBufComposer.storedEndIndex == ByteBufComposer.INITIAL_VALUE
        Assertions.assertThat(messageDecoder.byteBufComposer.components).containsOnly(new ByteBufComposer.Component())
        messageDecoder.@state == MessageDecoder.State.READY_TO_DECODE
        0 * _
    }

    def "should release underlying buffers after decoding multiple messages batched together in 2 packets"() {
        setup:
        ByteBuf encodedMessagePart1 = Unpooled.
                wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u00018=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=tes".getBytes(StandardCharsets.US_ASCII))
        ByteBuf encodedMessagePart2 = Unpooled.wrappedBuffer("t2\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart1)
        messageDecoder.channelRead(channelHandlerContext, encodedMessagePart2)

        then:
        2 * channelHandlerContext.fireChannelRead(fixMessage) >> channelHandlerContext
        encodedMessagePart1.refCnt() == 0
        encodedMessagePart2.refCnt() == 0
    }

    def "should close channel when byte buf composer is full"() {
        setup:
        (0..messageDecoder.@byteBufComposer.@components.length).forEach({ messageDecoder.@byteBufComposer.addByteBuf(Unpooled.buffer()) })
        Channel channel = Mock()
        Attribute sessionAttribute = Mock()
        NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(SessionConfig.builder().build(), new SessionID("", "", ""), TestSpec.INSTANCE)

        when:
        messageDecoder.channelRead(channelHandlerContext, Unpooled.buffer())

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.get() >> sessionState
        1 * channel.disconnect()
        0 * _
    }
}
