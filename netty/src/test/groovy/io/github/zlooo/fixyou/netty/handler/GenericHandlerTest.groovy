package io.github.zlooo.fixyou.netty.handler

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import spock.lang.Ignore
import spock.lang.Specification

import java.time.Clock

class GenericHandlerTest extends Specification {
    //TODO write this test

    private Clock clock = Mock()
    private GenericHandler genericHandler = new GenericHandler(clock)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()

    @Ignore("Class implementation not done yet")
    def "write tests when implementation is done"() {
        expect:
        assert false: "Test not implemented write it"
    }

    def "should close channel when exception is caught"() {
        when:
        genericHandler.exceptionCaught(channelHandlerContext, new RuntimeException())

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.close()
    }
}
