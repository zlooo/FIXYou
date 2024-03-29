package io.github.zlooo.fixyou.netty.handler.admin

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.SimpleFixMessage
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import spock.lang.Specification

class TestRequestHandlerTest extends Specification {

    private TestRequestHandler testRequestHandler = new TestRequestHandler()
    private ChannelHandlerContext channelHandlerContext = Mock()

    def "should send heartbeat request as response for test request"() {
        setup:
        FixMessage testRequest = new SimpleFixMessage()
        testRequest.setCharSequenceValue(FixConstants.TEST_REQ_ID_FIELD_NUMBER, "testRequestID")
        ChannelFuture channelFuture = Mock()

        when:
        testRequestHandler.handleMessage(testRequest, channelHandlerContext)

        then:
        1 * channelHandlerContext.writeAndFlush(testRequest) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        testRequest.refCnt() == 1 //+1 because testRequest is being reused as reply
        testRequest.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.HEARTBEAT
        testRequest.getCharSequenceValue(FixConstants.TEST_REQ_ID_FIELD_NUMBER).toString() == "testRequestID"
        0 * _
    }
}
