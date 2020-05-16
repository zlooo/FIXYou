package pl.zlooo.fixyou.netty.handler

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import pl.zlooo.fixyou.netty.handler.admin.AdministrativeMessageHandler
import pl.zlooo.fixyou.netty.handler.admin.TestSpec
import pl.zlooo.fixyou.parser.model.FixMessage
import spock.lang.Specification

class AdminMessagesHandlerTest extends Specification {

    private AdministrativeMessageHandler administrativeMessageHandler1 = Mock() {
        supportedMessageType() >> FixConstants.LOGON
    }
    private AdministrativeMessageHandler administrativeMessageHandler2 = Mock() {
        supportedMessageType() >> FixConstants.LOGOUT
    }
    private AdminMessagesHandler handler = new AdminMessagesHandler([administrativeMessageHandler1, administrativeMessageHandler2] as Set)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionStateAttribute = Mock()
    private NettyHandlerAwareSessionState sessionState = Mock()
    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)

    void setup() {
        fixMessage.retain()
    }

    def "should handle message of known type"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.LOGON

        when:
        handler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        1 * administrativeMessageHandler1.handleMessage(fixMessage, channelHandlerContext)
        0 * _
    }

    def "should handle message of unknown type"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.SEQUENCE_RESET

        when:
        handler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        fixMessage.refCnt() == 1
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        0 * _
    }

    def "should check for handler's supported message type uniqueness"() {
        setup:
        def handler1 = Mock(AdministrativeMessageHandler)
        def handler2 = Mock(AdministrativeMessageHandler)
        def handlers = new LinkedHashSet<>()
        handlers.add(handler1)
        handlers.add(handler2)

        when:
        new AdminMessagesHandler(handlers)

        then:
        thrown(IllegalArgumentException)
        2 * handler1.supportedMessageType() >> FixConstants.LOGON //second invocation is for exception message
        1 * handler2.supportedMessageType() >> FixConstants.LOGON
        0 * _
    }

    def "should do nothing if session is not established and message is not logon"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.SEQUENCE_RESET

        when:
        handler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get()
        0 * _
    }
}