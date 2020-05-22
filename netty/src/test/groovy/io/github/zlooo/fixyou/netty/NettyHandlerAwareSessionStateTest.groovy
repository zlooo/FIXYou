package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.session.MessageStore
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandler
import spock.lang.Specification

class NettyHandlerAwareSessionStateTest extends Specification {

    private io.github.zlooo.fixyou.parser.model.FixMessage fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(TestSpec.INSTANCE)
    private ChannelHandlerContext notMovingForwardOnReadAndWriteCtx = Mock()
    private ChannelOutboundHandler sessionHandler = Mock()

    def "should queue message when session is persistent"() {
        setup:
        MessageStore messageStore = Mock()
        def sessionID = new SessionID([] as char[], [] as char[], [] as char[])
        NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig().setPersistent(true).setMessageStore(messageStore), sessionID, Mock(DefaultObjectPool),
                                                                                       TestSpec.INSTANCE)
        sessionState.getResettables()[io.github.zlooo.fixyou.netty.handler.NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT] = notMovingForwardOnReadAndWriteCtx
        sessionState.getResettables()[io.github.zlooo.fixyou.netty.handler.NettyResettablesNames.SESSION] = sessionHandler
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 10L
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
        def sessionID = new SessionID([] as char[], [] as char[], [] as char[])
        NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig(), sessionID, Mock(DefaultObjectPool), TestSpec.INSTANCE)
        sessionState.getResettables()[io.github.zlooo.fixyou.netty.handler.NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT] = notMovingForwardOnReadAndWriteCtx
        sessionState.getResettables()[io.github.zlooo.fixyou.netty.handler.NettyResettablesNames.SESSION] = sessionHandler
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 10L
        fixMessage.retain()

        when:
        sessionState.queueMessage(fixMessage)

        then:
        1 * sessionHandler.write(notMovingForwardOnReadAndWriteCtx, fixMessage, null)
        fixMessage.refCnt() == 0
        0 * _
    }
}
