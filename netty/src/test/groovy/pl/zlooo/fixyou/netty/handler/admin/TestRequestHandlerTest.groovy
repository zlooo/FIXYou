package pl.zlooo.fixyou.netty.handler.admin

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.parser.model.FixMessage
import spock.lang.Specification

class TestRequestHandlerTest extends Specification {

    private TestRequestHandler testRequestHandler = new TestRequestHandler()
    private ChannelHandlerContext channelHandlerContext = Mock()

    def "should send heartbeat request as response for test request"() {
        setup:
        FixMessage testRequest = new FixMessage(TestSpec.INSTANCE)
        testRequest.getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).value = "testRequestID".toCharArray()
        ChannelFuture channelFuture = Mock()

        when:
        testRequestHandler.handleMessage(testRequest, channelHandlerContext)

        then:
        1 * channelHandlerContext.writeAndFlush(testRequest) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        testRequest.refCnt()==1
        testRequest.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.HEARTBEAT
        testRequest.getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).value == "testRequestID".toCharArray()
        0 * _
    }
}
