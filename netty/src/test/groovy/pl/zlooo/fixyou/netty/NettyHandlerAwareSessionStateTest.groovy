package pl.zlooo.fixyou.netty

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandler
import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool
import pl.zlooo.fixyou.netty.handler.NettyResettablesNames
import pl.zlooo.fixyou.netty.handler.admin.TestSpec
import pl.zlooo.fixyou.parser.model.FixMessage
import pl.zlooo.fixyou.session.MessageStore
import pl.zlooo.fixyou.session.SessionConfig
import pl.zlooo.fixyou.session.SessionID
import spock.lang.Specification

class NettyHandlerAwareSessionStateTest extends Specification {

    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)
    private ChannelHandlerContext notMovingForwardOnReadAndWriteCtx = Mock()
    private ChannelOutboundHandler sessionHandler = Mock()

    def "should queue message when session is persistent"() {
        setup:
        MessageStore messageStore = Mock()
        def sessionID = new SessionID([] as char[], [] as char[], [] as char[])
        NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig().setPersistent(true).setMessageStore(messageStore), sessionID, Mock(DefaultObjectPool),
                                                                                       TestSpec.INSTANCE)
        sessionState.getResettables()[NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT] = notMovingForwardOnReadAndWriteCtx
        sessionState.getResettables()[NettyResettablesNames.SESSION] = sessionHandler
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
        sessionState.getResettables()[NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT] = notMovingForwardOnReadAndWriteCtx
        sessionState.getResettables()[NettyResettablesNames.SESSION] = sessionHandler
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
