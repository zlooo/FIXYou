package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.SimpleFixMessage
import io.github.zlooo.fixyou.netty.handler.NettyResettablesNames
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.session.MessageStore
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandler
import spock.lang.Specification

class NettyHandlerAwareSessionStateTest extends Specification {

    private FixMessage fixMessage = new SimpleFixMessage()
    private ChannelHandlerContext notMovingForwardOnReadAndWriteCtx = Mock()
    private ChannelOutboundHandler sessionHandler = Mock()
    private SessionID sessionID = new SessionID("", "", "")

    def "should queue message when session is persistent"() {
        setup:
        MessageStore messageStore = Mock()
        NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(SessionConfig.builder().persistent(true).messageStore(messageStore).build(), sessionID, TestSpec.INSTANCE)
        sessionState.getResettables()[NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT] = notMovingForwardOnReadAndWriteCtx
        sessionState.getResettables()[NettyResettablesNames.SESSION] = sessionHandler
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 10L)
        fixMessage.retain() //message obtained from pool should have ref count of 1

        when:
        sessionState.queueMessage(fixMessage)

        then:
        1 * sessionHandler.write(notMovingForwardOnReadAndWriteCtx, fixMessage, null)
        1 * messageStore.storeMessage(sessionID, 10L, fixMessage) >> { args -> args[2].retain() }//message store should inc refCnt
        fixMessage.refCnt() == 1
        0 * _
    }

    def "should not queue message when session is not persistent"() {
        setup:
        NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(SessionConfig.builder().build(), sessionID, TestSpec.INSTANCE)
        sessionState.getResettables()[NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT] = notMovingForwardOnReadAndWriteCtx
        sessionState.getResettables()[NettyResettablesNames.SESSION] = sessionHandler
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 10L)
        fixMessage.retain() //message obtained from pool should have ref count of 1

        when:
        sessionState.queueMessage(fixMessage)

        then:
        1 * sessionHandler.write(notMovingForwardOnReadAndWriteCtx, fixMessage, null)
        fixMessage.refCnt() == 0
        0 * _
    }
}
