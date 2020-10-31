package io.github.zlooo.fixyou.netty.handler.admin

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import spock.lang.Specification

class TestRequestHandlerTest extends Specification {

    private TestRequestHandler testRequestHandler = new TestRequestHandler()
    private ChannelHandlerContext channelHandlerContext = Mock()

    def "should send heartbeat request as response for test request"() {
        setup:
        FixMessage testRequest = new FixMessage(new FieldCodec())
        testRequest.getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).charSequenceValue = "testRequestID".toCharArray()
        ChannelFuture channelFuture = Mock()

        when:
        testRequestHandler.handleMessage(testRequest, channelHandlerContext)

        then:
        1 * channelHandlerContext.writeAndFlush(testRequest) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        testRequest.refCnt() == 1
        testRequest.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.toString() == String.valueOf(FixConstants.HEARTBEAT)
        testRequest.getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).charSequenceValue.toString() == "testRequestID"
        0 * _
    }
}
