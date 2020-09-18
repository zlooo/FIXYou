package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.parser.model.FixMessage
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class SimplifiedMessageDecoderTest extends Specification {

    private SimplifiedMessageDecoder messageDecoder = new SimplifiedMessageDecoder()
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()

    def "should decode not fragmented message"() {
        setup:
        ByteBuf encodedMessage = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=test\u000110=023\u0001".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
        1 * channelHandlerContext.fireChannelRead(_ as FixMessage)
        0 * _
    }

    def "should close channel when fragmented message is received"() {
        setup:
        ByteBuf encodedMessage = Unpooled.wrappedBuffer("8=FIXT.1.1\u00019=28\u000149=sender\u000156=target\u000158=te".getBytes(StandardCharsets.US_ASCII))

        when:
        messageDecoder.channelRead(channelHandlerContext, encodedMessage)

        then:
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
