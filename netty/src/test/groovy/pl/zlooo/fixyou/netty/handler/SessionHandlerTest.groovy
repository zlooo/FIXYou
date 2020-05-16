package pl.zlooo.fixyou.netty.handler

import io.netty.channel.*
import org.assertj.core.api.Assertions
import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool
import pl.zlooo.fixyou.fix.commons.LogoutTexts
import pl.zlooo.fixyou.fix.commons.RejectReasons
import pl.zlooo.fixyou.fix.commons.utils.FixMessageUtils
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import pl.zlooo.fixyou.netty.handler.admin.TestSpec
import pl.zlooo.fixyou.netty.utils.FixChannelListeners
import pl.zlooo.fixyou.parser.model.FixMessage
import pl.zlooo.fixyou.session.SessionConfig
import pl.zlooo.fixyou.session.SessionID
import spock.lang.Specification

class SessionHandlerTest extends Specification {

    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock(DefaultObjectPool)
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig(), new SessionID("testBeginString".toCharArray(), "testSender".toCharArray(), "testTarget".toCharArray()), fixMessageObjectPool,
                                                                                           TestSpec.INSTANCE)
    private SessionHandler sessionHandler = new SessionHandler(sessionState)
    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private ChannelFuture channelFuture = Mock()

    void setup() {
        fixMessage.retain()
    }

    def "should reset state"() {
        setup:
        sessionHandler.@nextExpectedInboundSequenceNumber = 10L
        sessionHandler.@lastOutboundSequenceNumber = 10L
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(10L, fixMessage)

        when:
        sessionHandler.reset()

        then:
        fixMessage.refCnt() == 0
        sessionHandler.@nextExpectedInboundSequenceNumber == SessionHandler.DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        sessionHandler.@sequenceNumberToQueuedFixMessages.isEmpty()
        0 * _
    }

    def "should set new expected sequence number when sequence reset in reset mode is received"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.SEQUENCE_RESET
        fixMessage.getField(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).value = 666L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 0
        sessionHandler.@nextExpectedInboundSequenceNumber == 666L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        sessionHandler.@sequenceNumberToQueuedFixMessages.isEmpty()
        0 * _
    }

    def "should set new expected sequence number when sequence reset in gap fill mode is received"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.SEQUENCE_RESET
        fixMessage.getField(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER).value = true
        fixMessage.getField(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).value = 666L
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 1L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 0
        sessionHandler.@nextExpectedInboundSequenceNumber == 666L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        sessionHandler.@sequenceNumberToQueuedFixMessages.isEmpty()
        0 * _
    }

    def "should reject sequence reset in reset mode with lower sequence number than expected"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.SEQUENCE_RESET
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 444L
        fixMessage.getField(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).value = 666L
        sessionHandler.@nextExpectedInboundSequenceNumber = 777L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.refCnt() == 1
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.REJECT
        fixMessage.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER).value == 444L
        fixMessage.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).value == RejectReasons.VALUE_IS_INCORRECT_FOR_THIS_TAG
        fixMessage.getField(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER).value == FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value == RejectReasons.TOO_LOW_NEW_SEQUENCE_NUMBER
        0 * _
    }

    def "should increment next expected sequence number"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 1L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 2L
        0 * _
    }

    def "should push queued messages for processing"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 1L
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(2L, fixMessage)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(3L, fixMessage)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(5L, fixMessage)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 1
        3 * channelHandlerContext.fireChannelRead(fixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 4L
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).containsOnly(Map.entry(5L, fixMessage))
        0 * _
    }

    def "should ignore message with PosDupFlag set that have sequence number lower than expected"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 1L
        fixMessage.getField(FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER).value = true
        sessionHandler.@nextExpectedInboundSequenceNumber = 666L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 0
        sessionHandler.@nextExpectedInboundSequenceNumber == 666L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        sessionHandler.@sequenceNumberToQueuedFixMessages.isEmpty()
        0 * _
    }

    def "should terminate session when message without PosDupFlag and with lower than expected sequence number is received"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 1L
        sessionHandler.@nextExpectedInboundSequenceNumber = 666L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture
        fixMessage.refCnt() == 1
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.LOGOUT
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value == LogoutTexts.SEQUENCE_NUMBER_LOWER_THAN_EXPECTED
        0 * _
    }

    def "should send resend request when gap is detected"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 10L
        FixMessage resendRequest = new FixMessage(sessionHandler.sessionState.fixSpec)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * fixMessageObjectPool.getAndRetain() >> resendRequest
        1 * channelHandlerContext.writeAndFlush(resendRequest) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        resendRequest.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.RESEND_REQUEST
        resendRequest.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value == 1L
        resendRequest.getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).value == 1L
        resendRequest.getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).value == 9L
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).containsExactlyInAnyOrderEntriesOf([(1L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (2L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (3L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (4L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (5L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (6L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (7L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (8L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (9L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (10L): fixMessage])
        sessionHandler.@nextExpectedInboundSequenceNumber == SessionHandler.DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER
        sessionHandler.@lastOutboundSequenceNumber == 1L
        fixMessage.refCnt() == 1
        0 * _
    }

    def "should send resend request when single message gap is detected"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 2L
        FixMessage resendRequest = new FixMessage(sessionHandler.sessionState.fixSpec)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 1
        1 * fixMessageObjectPool.getAndRetain() >> resendRequest
        1 * channelHandlerContext.writeAndFlush(resendRequest) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        resendRequest.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.RESEND_REQUEST
        resendRequest.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value == 1L
        resendRequest.getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).value == 1L
        resendRequest.getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).value == 1L
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).containsExactlyInAnyOrderEntriesOf([(1L): FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (2L): fixMessage])
        sessionHandler.@nextExpectedInboundSequenceNumber == SessionHandler.DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER
        sessionHandler.@lastOutboundSequenceNumber == 1L
        0 * _
    }

    def "should push message for processing when response to resend request arrives"() {
        setup:
        FixMessage queuedFixMessage = new FixMessage(TestSpec.INSTANCE)
        queuedFixMessage.retain()
        sessionHandler.@sequenceNumberToQueuedFixMessages.putAll([(1L): FixMessageUtils.EMPTY_FAKE_MESSAGE, (2L): queuedFixMessage, (3L): FixMessageUtils.EMPTY_FAKE_MESSAGE])
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 1L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 1
        queuedFixMessage.refCnt() == 1
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        1 * channelHandlerContext.fireChannelRead(queuedFixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 3L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).containsOnly(Map.entry(3L, FixMessageUtils.EMPTY_FAKE_MESSAGE))
        0 * _
    }

    def "should send resend request cut down to what has not been yet requested"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 10L
        def queuedFixMessage = new FixMessage(TestSpec.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.putAll([(1L): FixMessageUtils.EMPTY_FAKE_MESSAGE, (2L): FixMessageUtils.EMPTY_FAKE_MESSAGE, (3L): queuedFixMessage])
        FixMessage resendRequest = new FixMessage(sessionHandler.sessionState.fixSpec)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 1
        1 * fixMessageObjectPool.getAndRetain() >> resendRequest
        1 * channelHandlerContext.writeAndFlush(resendRequest) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        resendRequest.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value == FixConstants.RESEND_REQUEST
        resendRequest.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value == 1L
        resendRequest.getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).value == 4L
        resendRequest.getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).value == 9L
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).containsExactlyInAnyOrderEntriesOf([(1L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (2L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (3L) : queuedFixMessage,
                                                                                                                     (4L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (5L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (6L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (7L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (8L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (9L) : FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (10L): fixMessage])
        sessionHandler.@nextExpectedInboundSequenceNumber == SessionHandler.DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER
        sessionHandler.@lastOutboundSequenceNumber == 1L
        0 * _
    }

    def "should not send resend request for sequence that has already been requested"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 2L
        def queuedFixMessage = new FixMessage(TestSpec.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.putAll([(1L): FixMessageUtils.EMPTY_FAKE_MESSAGE, (2L): FixMessageUtils.EMPTY_FAKE_MESSAGE, (3L): queuedFixMessage])

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 1
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).containsExactlyInAnyOrderEntriesOf([(1L): FixMessageUtils.EMPTY_FAKE_MESSAGE,
                                                                                                                     (2L): fixMessage,
                                                                                                                     (3L): queuedFixMessage])
        sessionHandler.@nextExpectedInboundSequenceNumber == SessionHandler.DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        0 * _
    }

    def "should set session id fields and outbound sequence number"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = "D".toCharArray()
        ChannelPromise channelPromise = Mock()

        when:
        sessionHandler.write(channelHandlerContext, fixMessage, channelPromise)

        then:
        fixMessage.refCnt() == 1
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value == 1L
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value == "testSender".toCharArray()
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value == "testTarget".toCharArray()
        1 * channelHandlerContext.write(fixMessage, channelPromise)
        sessionHandler.@lastOutboundSequenceNumber == 1L
        0 * _
    }

    def "should set session id fields but not outbound sequence number if sequence reset gap fill is sent"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.SEQUENCE_RESET
        fixMessage.getField(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER).value = true
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 666L
        ChannelPromise channelPromise = Mock()

        when:
        sessionHandler.write(channelHandlerContext, fixMessage, channelPromise)

        then:
        fixMessage.refCnt() == 1
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value == 666L
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value == "testSender".toCharArray()
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value == "testTarget".toCharArray()
        1 * channelHandlerContext.write(fixMessage, channelPromise)
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        0 * _
    }

    def "should set session id fields but not outbound sequence number if message has possible duplication flag set"() {
        setup:
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = "D".toCharArray()
        fixMessage.getField(FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER).value = true
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 666L
        ChannelPromise channelPromise = Mock()

        when:
        sessionHandler.write(channelHandlerContext, fixMessage, channelPromise)

        then:
        fixMessage.refCnt() == 1
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value == 666L
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value == "testSender".toCharArray()
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value == "testTarget".toCharArray()
        1 * channelHandlerContext.write(fixMessage, channelPromise)
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        0 * _
    }

    def "should reprocess queued message if it is sequence reset"() {
        setup:
        FixMessage sequenceReset = new FixMessage(TestSpec.INSTANCE)
        sequenceReset.retain()
        sequenceReset.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.SEQUENCE_RESET
        sequenceReset.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 2L
        sequenceReset.getField(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).value = 10L
        sessionHandler.@sequenceNumberToQueuedFixMessages.putAll([(1L): FixMessageUtils.EMPTY_FAKE_MESSAGE, (2L): sequenceReset])
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 1L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 1
        sequenceReset.refCnt() == 0
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 10L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).isEmpty()
        0 * _
    }

    def "should push message for processing when sequence reset - gap fill arrives"() {
        setup:
        FixMessage someFixMessage = new FixMessage(TestSpec.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.putAll([(1L): FixMessageUtils.EMPTY_FAKE_MESSAGE, (2L): FixMessageUtils.EMPTY_FAKE_MESSAGE, (3L): someFixMessage])
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 1L
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.SEQUENCE_RESET
        fixMessage.getField(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER).value = true
        fixMessage.getField(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).value = 3

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 0
        1 * channelHandlerContext.fireChannelRead(someFixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 4L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).isEmpty()
        0 * _
    }

    def "should push message for processing when sequence reset - reset arrives"() {
        setup:
        FixMessage someFixMessage = new FixMessage(TestSpec.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.putAll([(1L): FixMessageUtils.EMPTY_FAKE_MESSAGE, (2L): FixMessageUtils.EMPTY_FAKE_MESSAGE, (3L): someFixMessage])
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.SEQUENCE_RESET
        fixMessage.getField(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).value = 3

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 0
        1 * channelHandlerContext.fireChannelRead(someFixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 4L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).isEmpty()
        0 * _
    }

    def "should ignore queued admin messages other than sequence reset"() {
        setup:
        FixMessage someFixMessage = new FixMessage(TestSpec.INSTANCE)
        someFixMessage.retain()
        someFixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.HEARTBEAT
        sessionHandler.@sequenceNumberToQueuedFixMessages.putAll([(1L): FixMessageUtils.EMPTY_FAKE_MESSAGE, (2L): someFixMessage])
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value = 1L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 1
        someFixMessage.refCnt() == 0
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 3L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).isEmpty()
        0 * _
    }

    def "should disconnect session when channel becomes inactive"() {
        setup:
        Channel channel = Mock()
        sessionState.channel = channel
        sessionState.logonSent = true
        sessionState.logoutSent = true

        when:
        sessionHandler.channelInactive(channelHandlerContext)

        then:
        1 * channelHandlerContext.channel() >> channel
        !sessionState.getConnected().get()
        sessionState.channel == channel
        !sessionState.logonSent
        sessionState.logoutSent
        1 * channelHandlerContext.fireChannelInactive()
        0 * _
    }
}
