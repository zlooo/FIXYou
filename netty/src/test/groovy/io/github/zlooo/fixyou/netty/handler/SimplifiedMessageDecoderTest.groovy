package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.FIXYouException
import io.github.zlooo.fixyou.commons.pool.ObjectPool
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.SimpleFixMessage
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class SimplifiedMessageDecoderTest extends Specification {

    private ObjectPool<FixMessage> fixMessagePool = Mock()
    private SimplifiedMessageDecoder messageDecoder = new SimplifiedMessageDecoder(fixMessagePool)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private FixMessage fixMessage = new SimpleFixMessage()

    def "should decode not fragmented message"() {
        setup:
        ByteBuf encodedMessage = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))
        fixMessage.retain() //message obtained from pool should have ref count of 1

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        1 * fixMessagePool.tryGetAndRetain() >> fixMessage
        1 * channelHandlerContext.fireChannelRead({
            verifyAll(it, FixMessage) { decodedMessage ->
                decodedMessage.isValueSet(8)
                decodedMessage.isValueSet(10)
            }
        })
        encodedMessage.refCnt() == 0
        0 * _
    }

    def "should error out because of fix message inavailability in pool"() {
        setup:
        ByteBuf encodedMessage = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        SimplifiedMessageDecoder.GET_FROM_POOL_MAX_ATTEMPTS * fixMessagePool.tryGetAndRetain() >> null
        thrown(FIXYouException)
        0 * _
    }

    def "should close channel when fragmented message is received"() {
        setup:
        ByteBuf encodedMessage = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))
        fixMessage.retain() //message obtained from pool should have ref count of 1

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        1 * fixMessagePool.tryGetAndRetain() >> fixMessage
        1 * channelHandlerContext.channel() >> channel
        1 * channelHandlerContext.close()
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

    def "should pass user event further"() {
        setup:
        def event = "event"

        when:
        messageDecoder.userEventTriggered(channelHandlerContext, event)

        then:
        1 * channelHandlerContext.fireUserEventTriggered(event)
        0 * _
    }

    def "should pass writability change info"() {
        when:
        messageDecoder.channelWritabilityChanged(channelHandlerContext)

        then:
        1 * channelHandlerContext.fireChannelWritabilityChanged()
        0 * _
    }
}
