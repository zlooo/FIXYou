package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.MessageStore
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import spock.lang.Specification

class MessageStoreHandlerTest extends Specification {

    private MessageStore messageStore = Mock()
    private SessionID sessionID = new SessionID([] as char[], 0, [] as char[], 0, [] as char[], 0)
    private MessageStoreHandler messageStoreHandler = new MessageStoreHandler(sessionID, messageStore)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private Attribute sessionStateAttribute = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig(), sessionID, Mock(DefaultObjectPool), Mock(DefaultObjectPool), TestSpec.INSTANCE)

    def "should store message if session is persistent"() {
        setup:
        sessionState.getSessionConfig().setPersistent(true)
        FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 666L

        when:
        messageStoreHandler.write(channelHandlerContext, fixMessage, null)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        1 * messageStore.storeMessage(sessionID, 666L, fixMessage)
        1 * channelHandlerContext.write(fixMessage, null)
        0 * _
    }

    def "should pass message if session is not established"() {
        setup:
        sessionState.getSessionConfig().setPersistent(false)
        FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 666L

        when:
        messageStoreHandler.write(channelHandlerContext, fixMessage, null)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> null
        1 * channelHandlerContext.write(fixMessage, null)
        0 * _
    }

    def "should pass message if object passed is not FixMessage"() {
        setup:
        sessionState.getSessionConfig().setPersistent(false)
        Object object = new Object()

        when:
        messageStoreHandler.write(channelHandlerContext, object, null)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> null
        1 * channelHandlerContext.write(object, null)
        0 * _
    }

    def "should reset underlying store when handler is reset"() {
        when:
        messageStoreHandler.reset()

        then:
        1 * messageStore.reset(sessionID)
        0 * _
    }
}
