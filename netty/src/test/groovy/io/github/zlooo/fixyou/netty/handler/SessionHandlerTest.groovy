package io.github.zlooo.fixyou.netty.handler

import com.carrotsearch.hppcrt.cursors.LongObjectCursor
import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.memory.RegionPool
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.fix.commons.LogoutTexts
import io.github.zlooo.fixyou.fix.commons.RejectReasons
import io.github.zlooo.fixyou.fix.commons.utils.EmptyFixMessage
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.SimpleFixMessage
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.netty.utils.FixChannelListeners
import io.github.zlooo.fixyou.parser.model.OffHeapFixMessage
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.channel.*
import org.assertj.core.api.Assertions
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class SessionHandlerTest extends Specification {

    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock(DefaultObjectPool)
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(SessionConfig.builder().build(), new SessionID("testBeginString", "testSender", "testTarget"), TestSpec.INSTANCE)
    private SessionHandler sessionHandler = new SessionHandler(sessionState, fixMessageObjectPool)
    @Shared
    private RegionPool regionPool = new RegionPool(16, 256 as short)
    @AutoCleanup
    private FixMessage fixMessage = new OffHeapFixMessage(regionPool)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private ChannelFuture channelFuture = Mock()

    void setup() {
        fixMessage.retain() //message processed by this handler should have refCnt = 1
    }

    void cleanupSpec() {
        regionPool.close()
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
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.SEQUENCE_RESET)
        fixMessage.setLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER, 666L)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        sessionHandler.@nextExpectedInboundSequenceNumber == 666L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        sessionHandler.@sequenceNumberToQueuedFixMessages.isEmpty()
        0 * _
    }

    def "should set new expected sequence number when sequence reset in gap fill mode is received"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.SEQUENCE_RESET)
        fixMessage.setBooleanValue(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER, true)
        fixMessage.setLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER, 666L)
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 1L)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        sessionHandler.@nextExpectedInboundSequenceNumber == 666L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        sessionHandler.@sequenceNumberToQueuedFixMessages.isEmpty()
        0 * _
    }

    def "should reject sequence reset in reset mode with lower sequence number than expected"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.SEQUENCE_RESET)
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 444L)
        fixMessage.setLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER, 666L)
        sessionHandler.@nextExpectedInboundSequenceNumber = 777L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.refCnt() == 1 + 1 //1 because NotPoolableFixMessage after creation has refCnt == 1 and +1 because fixMessage is being reused as reject
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.REJECT)
        fixMessage.getLongValue(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER) == 444L
        fixMessage.getLongValue(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER) == RejectReasons.VALUE_IS_INCORRECT_FOR_THIS_TAG
        fixMessage.getLongValue(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER) == FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER
        fixMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == String.valueOf(RejectReasons.TOO_LOW_NEW_SEQUENCE_NUMBER)
        0 * _
    }

    def "should increment next expected sequence number"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 1L)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 2L
        0 * _
    }

    def "should push queued messages for processing"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 1L)
        fixMessage.retain()
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(2L, fixMessage)
        fixMessage.retain()
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(3L, fixMessage)
        fixMessage.retain()
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(5L, fixMessage)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        fixMessage.refCnt() == 1 + 1 //1 because NotPoolableFixMessage after creation has refCnt == 1 and +1 because fixMessage with sequence number 5 should not be sent
        3 * channelHandlerContext.fireChannelRead(fixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 4L
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).usingElementComparatorIgnoringFields("index").containsOnly(new LongObjectCursor<>(key: 5L, value: fixMessage))
        0 * _
    }

    def "should ignore message with PosDupFlag set that have sequence number lower than expected"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 1L)
        fixMessage.setBooleanValue(FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER, true)
        sessionHandler.@nextExpectedInboundSequenceNumber = 666L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        sessionHandler.@nextExpectedInboundSequenceNumber == 666L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        sessionHandler.@sequenceNumberToQueuedFixMessages.isEmpty()
        0 * _
    }

    def "should terminate session when message without PosDupFlag and with lower than expected sequence number is received"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 1L)
        sessionHandler.@nextExpectedInboundSequenceNumber = 666L

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture
        fixMessage.refCnt() == 1 + 1 //1 because NotPoolableFixMessage after creation has refCnt == 1 and +1 because fixMessage is being reused as logout
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.LOGOUT)
        fixMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == String.valueOf(LogoutTexts.SEQUENCE_NUMBER_LOWER_THAN_EXPECTED)
        0 * _
    }

    def "should send resend request when gap is detected"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 10L)
        fixMessage.setCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER, "some value")
        FixMessage resendRequest = new OffHeapFixMessage(regionPool)
        FixMessage messageToQueue = new OffHeapFixMessage(regionPool)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        2 * fixMessageObjectPool.getAndRetain() >> resendRequest >> messageToQueue
        1 * channelHandlerContext.writeAndFlush(resendRequest) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        resendRequest.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.RESEND_REQUEST)
        resendRequest.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 1L
        resendRequest.getLongValue(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER) == 1L
        resendRequest.getLongValue(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER) == 9L
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).hasSize(10)
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(1) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(2) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(3) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(4) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(5) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(6) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(7) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(8) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(9) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(10) == messageToQueue
        messageToQueue.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).toString() == "some value"
        sessionHandler.@nextExpectedInboundSequenceNumber == SessionHandler.DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER
        sessionHandler.@lastOutboundSequenceNumber == 1L
        0 * _

        cleanup:
        resendRequest?.close()
        messageToQueue?.close()
    }

    def "should send resend request when single message gap is detected"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 2L)
        FixMessage resendRequest = new OffHeapFixMessage(regionPool)
        FixMessage messageToQueue = new OffHeapFixMessage(regionPool)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        2 * fixMessageObjectPool.getAndRetain() >> resendRequest >> messageToQueue
        1 * channelHandlerContext.writeAndFlush(resendRequest) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        resendRequest.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.RESEND_REQUEST)
        resendRequest.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 1L
        resendRequest.getLongValue(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER) == 1L
        resendRequest.getLongValue(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER) == 1L
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).hasSize(2)
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(1) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(2) == messageToQueue
        sessionHandler.@nextExpectedInboundSequenceNumber == SessionHandler.DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER
        sessionHandler.@lastOutboundSequenceNumber == 1L
        0 * _

        cleanup:
        resendRequest?.close()
        messageToQueue?.close()
    }

    def "should push message for processing when response to resend request arrives"() {
        setup:
        FixMessage queuedFixMessage = new SimpleFixMessage()
        queuedFixMessage.retain() //should have refCnt = 1 when queued
        queuedFixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(1L, EmptyFixMessage.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(2L, queuedFixMessage)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(3L, EmptyFixMessage.INSTANCE)
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 1L)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        queuedFixMessage.refCnt() == 0
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        1 * channelHandlerContext.fireChannelRead(queuedFixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 3L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).usingElementComparatorIgnoringFields("index").containsOnly(Assertions.entry(3L, EmptyFixMessage.INSTANCE))
        0 * _

        cleanup:
        queuedFixMessage?.close()
    }

    def "should send resend request cut down to what has not been yet requested"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 10L)
        def queuedFixMessage = new OffHeapFixMessage(regionPool)
        queuedFixMessage.retain() //should have refCnt = 1 when queued
        sessionHandler.@sequenceNumberToQueuedFixMessages.putAll(
                [new LongObjectCursor<>(key: 1, value: EmptyFixMessage.INSTANCE), new LongObjectCursor<>(key: 2L, value: EmptyFixMessage.INSTANCE), new LongObjectCursor<>(key: 3L, value: queuedFixMessage)])
        FixMessage resendRequest = new OffHeapFixMessage(regionPool)
        FixMessage messageToQueue = new OffHeapFixMessage(regionPool)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        2 * fixMessageObjectPool.getAndRetain() >> resendRequest >> messageToQueue
        1 * channelHandlerContext.writeAndFlush(resendRequest) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        resendRequest.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.RESEND_REQUEST)
        resendRequest.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 1L
        resendRequest.getLongValue(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER) == 4L
        resendRequest.getLongValue(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER) == 9L
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).hasSize(10)
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(1) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(2) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(3) == queuedFixMessage
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(4) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(5) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(6) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(7) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(8) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(9) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(10) == messageToQueue
        sessionHandler.@nextExpectedInboundSequenceNumber == SessionHandler.DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER
        sessionHandler.@lastOutboundSequenceNumber == 1L
        0 * _

        cleanup:
        queuedFixMessage?.close()
        resendRequest?.close()
        messageToQueue?.close()
    }

    def "should not send resend request for sequence that has already been requested"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 2L)
        def queuedFixMessage = new OffHeapFixMessage(regionPool)
        queuedFixMessage.retain() //should have refCnt = 1 when queued
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(1L, EmptyFixMessage.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(2L, EmptyFixMessage.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(3L, queuedFixMessage)
        FixMessage messageToQueue = new OffHeapFixMessage(regionPool)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * fixMessageObjectPool.getAndRetain() >> messageToQueue
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).hasSize(3)
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(1) == EmptyFixMessage.INSTANCE
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(2) == messageToQueue
        sessionHandler.@sequenceNumberToQueuedFixMessages.get(3) == queuedFixMessage
        sessionHandler.@nextExpectedInboundSequenceNumber == SessionHandler.DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        0 * _

        cleanup:
        queuedFixMessage?.close()
        messageToQueue?.close()
    }

    def "should set session id fields and outbound sequence number"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        ChannelPromise channelPromise = Mock()

        when:
        sessionHandler.write(channelHandlerContext, fixMessage, channelPromise)

        then:
        fixMessage.refCnt() == 1
        fixMessage.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 1L
        fixMessage.getCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).toString() == "testSender"
        fixMessage.getCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).toString() == "testTarget"
        1 * channelHandlerContext.write(fixMessage, channelPromise)
        sessionHandler.@lastOutboundSequenceNumber == 1L
        0 * _
    }

    def "should set session id fields but not outbound sequence number if sequence reset gap fill is sent"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.SEQUENCE_RESET)
        fixMessage.setBooleanValue(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER, true)
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 666L)
        ChannelPromise channelPromise = Mock()

        when:
        sessionHandler.write(channelHandlerContext, fixMessage, channelPromise)

        then:
        fixMessage.refCnt() == 1
        fixMessage.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 666L
        fixMessage.getCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).toString() == "testSender"
        fixMessage.getCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).toString() == "testTarget"
        1 * channelHandlerContext.write(fixMessage, channelPromise)
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        0 * _
    }

    def "should set session id fields but not outbound sequence number if message has possible duplication flag set"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setBooleanValue(FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER, true)
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 666L)
        ChannelPromise channelPromise = Mock()

        when:
        sessionHandler.write(channelHandlerContext, fixMessage, channelPromise)

        then:
        fixMessage.refCnt() == 1
        fixMessage.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 666L
        fixMessage.getCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).toString() == "testSender"
        fixMessage.getCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).toString() == "testTarget"
        1 * channelHandlerContext.write(fixMessage, channelPromise)
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        0 * _
    }

    def "should reprocess queued message if it is sequence reset"() {
        setup:
        FixMessage sequenceReset = new SimpleFixMessage()
        sequenceReset.retain() //should have refCnt = 1 when queued
        sequenceReset.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.SEQUENCE_RESET)
        sequenceReset.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 2L)
        sequenceReset.setLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER, 10L)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(1L, EmptyFixMessage.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(2L, sequenceReset)
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 1L)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        sequenceReset.refCnt() == 0
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 10L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).isEmpty()
        0 * _

        cleanup:
        sequenceReset?.close()
    }

    def "should push message for processing when sequence reset - gap fill arrives"() {
        setup:
        FixMessage someFixMessage = new SimpleFixMessage()
        someFixMessage.retain() //should have refCnt = 1 when queued
        someFixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(1L, EmptyFixMessage.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(2L, EmptyFixMessage.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(3L, someFixMessage)
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 1L)
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.SEQUENCE_RESET)
        fixMessage.setBooleanValue(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER, true)
        fixMessage.setLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER, 3)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.fireChannelRead(someFixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 4L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).isEmpty()
        someFixMessage.refCnt() == 0
        0 * _

        cleanup:
        someFixMessage?.close()
    }

    def "should push message for processing when sequence reset - reset arrives"() {
        setup:
        FixMessage someFixMessage = new SimpleFixMessage()
        someFixMessage.retain() //should have refCnt = 1 when queued
        someFixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(1L, EmptyFixMessage.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(2L, EmptyFixMessage.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(3L, someFixMessage)
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.SEQUENCE_RESET)
        fixMessage.setLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER, 3)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.fireChannelRead(someFixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 4L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).isEmpty()
        someFixMessage.refCnt() == 0
        0 * _

        cleanup:
        someFixMessage?.close()
    }

    def "should ignore queued admin messages other than sequence reset"() {
        setup:
        FixMessage queuedHeartBeat = new SimpleFixMessage()
        queuedHeartBeat.retain() //should have refCnt = 1 when queued
        queuedHeartBeat.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.HEARTBEAT)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(1L, EmptyFixMessage.INSTANCE)
        sessionHandler.@sequenceNumberToQueuedFixMessages.put(2L, queuedHeartBeat)
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 1L)

        when:
        sessionHandler.channelRead(channelHandlerContext, fixMessage)

        then:
        queuedHeartBeat.refCnt() == 0
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        sessionHandler.@nextExpectedInboundSequenceNumber == 3L
        sessionHandler.@lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER
        Assertions.assertThat(sessionHandler.@sequenceNumberToQueuedFixMessages).isEmpty()
        0 * _

        cleanup:
        queuedHeartBeat
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
