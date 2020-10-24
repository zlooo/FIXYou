package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.handler.NettyResettablesNames
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.MessageStore
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandler
import spock.lang.Specification

class NettyHandlerAwareSessionStateTest extends Specification {

    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
    private ChannelHandlerContext notMovingForwardOnReadAndWriteCtx = Mock()
    private ChannelOutboundHandler sessionHandler = Mock()

    def "should queue message when session is persistent"() {
        setup:
        MessageStore messageStore = Mock()
        def sessionID = new SessionID([] as char[], 0, [] as char[], 0, [] as char[], 0)
        NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig().setPersistent(true).setMessageStore(messageStore), sessionID, Mock(DefaultObjectPool), Mock(DefaultObjectPool), TestSpec.INSTANCE)
        sessionState.getResettables()[NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT] = notMovingForwardOnReadAndWriteCtx
        sessionState.getResettables()[NettyResettablesNames.SESSION] = sessionHandler
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 10L
        fixMessage.retain()

        when:
        sessionState.queueMessage(fixMessage)

        then:
        1 * sessionHandler.write(notMovingForwardOnReadAndWriteCtx, fixMessage, null)
        1 * messageStore.storeMessage(sessionID, 10L, fixMessage)
        fixMessage.refCnt() == 0
        0 * _
    }

    def "should not queue message when session is not persistent"() {
        setup:
        def sessionID = new SessionID([] as char[], 0, [] as char[], 0, [] as char[], 0)
        NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig(), sessionID, Mock(DefaultObjectPool), Mock(DefaultObjectPool), TestSpec.INSTANCE)
        sessionState.getResettables()[NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT] = notMovingForwardOnReadAndWriteCtx
        sessionState.getResettables()[NettyResettablesNames.SESSION] = sessionHandler
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).booleanValue = 10L
        fixMessage.retain()

        when:
        sessionState.queueMessage(fixMessage)

        then:
        1 * sessionHandler.write(notMovingForwardOnReadAndWriteCtx, fixMessage, null)
        fixMessage.refCnt() == 0
        0 * _
    }
}
