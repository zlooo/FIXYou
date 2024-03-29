package io.github.zlooo.fixyou.netty.handler;

import com.carrotsearch.hppcrt.LongObjectMap;
import com.carrotsearch.hppcrt.maps.LongObjectHashMap;
import com.carrotsearch.hppcrt.procedures.ObjectProcedure;
import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.AbstractPoolableFixMessage;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.fix.commons.LogoutTexts;
import io.github.zlooo.fixyou.fix.commons.RejectReasons;
import io.github.zlooo.fixyou.fix.commons.session.SessionIDUtils;
import io.github.zlooo.fixyou.fix.commons.utils.EmptyFixMessage;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.netty.utils.FixChannelListeners;
import io.github.zlooo.fixyou.session.SessionID;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ChannelHandler.Sharable
class SessionHandler extends ChannelDuplexHandler implements SessionAwareChannelInboundHandler, Resettable {

    private static final int DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER = 1;
    private static final int DEFAULT_OUTBOUND_SEQUENCE_NUMBER = 0;

    private final NettyHandlerAwareSessionState sessionState;
    private final ObjectPool<? extends AbstractPoolableFixMessage> fixMessageObjectPool;
    private final SessionID sessionId;
    //TODO performance test it, maybe some other collection is better suited, especially since it's ordered
    private final LongObjectMap<FixMessage> sequenceNumberToQueuedFixMessages = new LongObjectHashMap<>(DefaultConfiguration.QUEUED_MESSAGES_MAP_SIZE);
    private long nextExpectedInboundSequenceNumber = DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER;
    private long lastOutboundSequenceNumber = DEFAULT_OUTBOUND_SEQUENCE_NUMBER;

    public SessionHandler(NettyHandlerAwareSessionState sessionState, ObjectPool<? extends AbstractPoolableFixMessage> fixMessageObjectPool) {
        this.sessionState = sessionState;
        this.fixMessageObjectPool = fixMessageObjectPool;
        this.sequenceNumberToQueuedFixMessages.setDefaultValue(EmptyFixMessage.INSTANCE);
        sessionId = sessionState.getSessionId();
        loadSequenceNumbers();
        log.debug("Created fix session handler for session {}", sessionId);
    }

    private void loadSequenceNumbers() {
        //TODO if persistence is configured load last sequence numbers from underlying store
    }

    @Override
    public void reset() {
        nextExpectedInboundSequenceNumber = DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER;
        lastOutboundSequenceNumber = DEFAULT_OUTBOUND_SEQUENCE_NUMBER;
        sequenceNumberToQueuedFixMessages.values().forEach((ObjectProcedure<? super FixMessage>) ReferenceCountUtil::release);
        sequenceNumberToQueuedFixMessages.clear();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FixMessage) {
            final FixMessage fixMessage = (FixMessage) msg;
            final boolean isSequenceReset = FixMessageUtils.isSequenceReset(fixMessage);
            if (isSequenceReset && !FixMessageUtils.hasBooleanFieldSet(fixMessage, FixConstants.GAP_FILL_FLAG_FIELD_NUMBER)) {
                setNewExpectedSeqNumber(ctx, fixMessage);
            } else {
                checkSequenceNumber(ctx, fixMessage, isSequenceReset);
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Channel {} has become inactive", ctx.channel());
        sessionState.getConnected().set(false);
        sessionState.setLogonSent(false);
        ctx.fireChannelInactive();
    }

    private void checkSequenceNumber(ChannelHandlerContext ctx, FixMessage fixMessage, boolean isSequenceReset) {
        final long sequenceNumberFromMessage = fixMessage.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER);
        if (sequenceNumberFromMessage == nextExpectedInboundSequenceNumber) {
            if (!sequenceNumberToQueuedFixMessages.isEmpty()) {
                sequenceNumberToQueuedFixMessages.remove(sequenceNumberFromMessage);
            }
            if (isSequenceReset) {
                setNewExpectedSeqNumber(ctx, fixMessage);
            } else {
                nextExpectedInboundSequenceNumber++;
                ctx.fireChannelRead(fixMessage);
                pushQueuedMessagesIfPresent(ctx);
            }
        } else if (sequenceNumberFromMessage < nextExpectedInboundSequenceNumber) {
            handleSequenceLowerThanExpected(ctx, fixMessage, nextExpectedInboundSequenceNumber);
        } else {
            handleSequenceHigherThanExpected(ctx, fixMessage, sequenceNumberFromMessage);
        }
    }

    private void setNewExpectedSeqNumber(ChannelHandlerContext ctx, FixMessage fixMessage) {
        final long newSequenceValue = fixMessage.getLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER);
        if (newSequenceValue < nextExpectedInboundSequenceNumber) {
            final FixMessage rejectMessage = FixMessageUtils.toRejectMessage(fixMessage, RejectReasons.VALUE_IS_INCORRECT_FOR_THIS_TAG, FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, RejectReasons.TOO_LOW_NEW_SEQUENCE_NUMBER);
            SessionIDUtils.setSessionIdFields(rejectMessage, sessionId);
            rejectMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, ++lastOutboundSequenceNumber);
            ctx.writeAndFlush(ReferenceCountUtil.retain(rejectMessage)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            log.warn("Sequence reset in reset mode received for session {}, setting new next expected inbound sequence number to {}", sessionId, newSequenceValue);
            for (long i = nextExpectedInboundSequenceNumber; i < newSequenceValue; i++) {
                sequenceNumberToQueuedFixMessages.remove(i);
            }
            nextExpectedInboundSequenceNumber = newSequenceValue;
            pushQueuedMessagesIfPresent(ctx);
        }
    }

    @SneakyThrows
    private void pushQueuedMessagesIfPresent(ChannelHandlerContext ctx) {
        if (!sequenceNumberToQueuedFixMessages.isEmpty()) { //quick path, 99% cases there won't be anything queued
            FixMessage queuedFixMessage;
            while ((queuedFixMessage = sequenceNumberToQueuedFixMessages.get(nextExpectedInboundSequenceNumber)) != EmptyFixMessage.INSTANCE) {
                try {
                    sequenceNumberToQueuedFixMessages.remove(nextExpectedInboundSequenceNumber);
                    if (!FixMessageUtils.isAdminMessage(queuedFixMessage)) {
                        nextExpectedInboundSequenceNumber++;
                        ctx.fireChannelRead(queuedFixMessage);
                    } else if (FixMessageUtils.isSequenceReset(queuedFixMessage)) {
                        channelRead(ctx, queuedFixMessage);
                    } else {
                        nextExpectedInboundSequenceNumber++;
                    }
                } finally {
                    ReferenceCountUtil.release(queuedFixMessage);
                }
            }
        }
    }

    private void handleSequenceHigherThanExpected(ChannelHandlerContext ctx, FixMessage fixMessage, long sequenceNumberFromMessage) {
        final long toInclusive = sequenceNumberFromMessage - 1;
        if (!hasResendBeenSent(nextExpectedInboundSequenceNumber, toInclusive)) {
            final long cutDownFromInclusive = cutDownFrom(nextExpectedInboundSequenceNumber);
            final long cutDownToInclusive = cutDownTo(toInclusive);
            if (cutDownFromInclusive <= cutDownToInclusive) {
                fillMapWithPlaceholders(cutDownFromInclusive, cutDownToInclusive);
                log.info("Message gap detected in session {}, sending resend request for sequence numbers <{}, {}>", sessionId, cutDownFromInclusive, cutDownToInclusive);
                final FixMessage resendRequest = FixMessageUtils.toResendRequest(fixMessageObjectPool.getAndRetain(), cutDownFromInclusive, cutDownToInclusive);
                SessionIDUtils.setSessionIdFields(resendRequest, sessionId);
                resendRequest.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, ++lastOutboundSequenceNumber);
                ctx.writeAndFlush(resendRequest).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            } else {
                log.error("Missing from sequence number({}) is lower than missing to sequence number({}), how can this be?", cutDownFromInclusive, cutDownToInclusive);
            }
        }
        final AbstractPoolableFixMessage fixMessageToBeQueued = fixMessageObjectPool.getAndRetain();
        fixMessageToBeQueued.copyDataFrom(fixMessage);
        sequenceNumberToQueuedFixMessages.put(sequenceNumberFromMessage, fixMessageToBeQueued);
        pushQueuedMessagesIfPresent(ctx);
    }

    private void handleSequenceLowerThanExpected(ChannelHandlerContext ctx, FixMessage fixMessage, long expectedSequenceNumber) {
        if (!FixMessageUtils.hasBooleanFieldSet(fixMessage, FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER)) {
            /*
             * According to fix specification:
             * "If the incoming message has a sequence number less than expected and the PossDupFlag is not set, it indicates a serious error. It is strongly recommended that
             * the session be terminated and manual intervention be initiated."
             * Ok I'll follow this recommendation then
             */
            log.error("Sequence number for session {} is lower than expected({}) and PossDupFlag is not set, terminating this session", sessionId, expectedSequenceNumber);
            final FixMessage logoutMessage = FixMessageUtils.toLogoutMessage(fixMessage, LogoutTexts.SEQUENCE_NUMBER_LOWER_THAN_EXPECTED);
            SessionIDUtils.setSessionIdFields(logoutMessage, sessionId);
            logoutMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, ++lastOutboundSequenceNumber);
            ctx.writeAndFlush(ReferenceCountUtil.retain(logoutMessage))
               .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
               .addListener(FixChannelListeners.LOGOUT_SENT);
        } else {
            log.debug("Got message with PosDupFlag that has lower than expected sequence number, ignoring it");
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FixMessage) {
            final FixMessage fixMessage = (FixMessage) msg;
            if (!((FixMessageUtils.isSequenceReset(fixMessage) && FixMessageUtils.hasBooleanFieldSet(fixMessage, FixConstants.GAP_FILL_FLAG_FIELD_NUMBER)) ||
                  FixMessageUtils.hasBooleanFieldSet(fixMessage, FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER))) {
                fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, ++lastOutboundSequenceNumber);
            }
            SessionIDUtils.setSessionIdFields(fixMessage, sessionId);
        }
        super.write(ctx, msg, promise);
    }

    private void fillMapWithPlaceholders(long fromInclusive, long toInclusive) {
        for (long i = fromInclusive; i <= toInclusive; i++) {
            sequenceNumberToQueuedFixMessages.put(i, EmptyFixMessage.INSTANCE);
        }
    }

    private long cutDownTo(long toInclusiveCandidate) {
        long to = toInclusiveCandidate;
        while (sequenceNumberToQueuedFixMessages.containsKey(to)) {
            to--;
        }
        return to;
    }

    private long cutDownFrom(long fromInclusiveCandidate) {
        long from = fromInclusiveCandidate;
        while (sequenceNumberToQueuedFixMessages.containsKey(from)) {
            from++;
        }
        return from;
    }

    private boolean hasResendBeenSent(long fromInclusive, long toInclusive) {
        return sequenceNumberToQueuedFixMessages.containsKey(toInclusive) && sequenceNumberToQueuedFixMessages.containsKey(fromInclusive);
    }
}
