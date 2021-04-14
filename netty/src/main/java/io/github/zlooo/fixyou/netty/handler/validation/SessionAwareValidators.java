package io.github.zlooo.fixyou.netty.handler.validation;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.utils.Comparators;
import io.github.zlooo.fixyou.fix.commons.LogoutTexts;
import io.github.zlooo.fixyou.fix.commons.RejectReasons;
import io.github.zlooo.fixyou.fix.commons.session.SessionIDUtils;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.netty.utils.FixChannelListeners;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.session.SessionID;
import io.github.zlooo.fixyou.session.ValidationConfig;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.channel.ChannelFutureListener;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.util.Arrays;

@Slf4j
@UtilityClass
public class SessionAwareValidators {

    public static final PredicateWithValidator<TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState>> ORIG_SENDING_TIME_VALIDATOR =
            new PredicateWithValidator<>(ValidationConfig::isShouldCheckOrigVsSendingTime, (fixMsg, sessionState) -> {
                if (!ValidationOperations.checkOrigSendingTime(fixMsg)) {
                    log.warn("Original sending time is greater than sending time in session {}. Logging out session, message will be logged on debug level", sessionState.getSessionId());
                    log.debug("Message with wrong OrigSendingTime {}", fixMsg);
                    return (ctx, message, fixMessageObjectPool) -> {
                        ctx.write(FixMessageUtils.toRejectMessage(message, RejectReasons.SENDING_TIME_ACCURACY_PROBLEM).retain()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                        ctx.writeAndFlush(FixMessageUtils.toLogoutMessage(fixMessageObjectPool.getAndRetain(), LogoutTexts.INACCURATE_SENDING_TIME))
                           .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                           .addListener(FixChannelListeners.LOGOUT_SENT)
                           .addListener(ChannelFutureListener.CLOSE);
                    };
                } else {
                    return null;
                }
            });

    public static final PredicateWithValidator<TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState>> COMP_ID_VALIDATOR = createCompIdValidator();

    public static final PredicateWithValidator<TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState>> BEGIN_STRING_VALIDATOR = createBeginStringValidator();

    public static final PredicateWithValidator<TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState>> BODY_LENGTH_VALIDATOR =
            new PredicateWithValidator<>(ValidationConfig::isShouldCheckBodyLength, (fixMessage, sessionState) -> {
                //TODO how do I get message length here? Will have to figure that out, for now disabling this check
                //                final Field bodyLengthField = fixMessage.getField(FixConstants.BODY_LENGTH_FIELD_NUMBER);
                //                final long bodyLength = bodyLengthField.getLongValue();
                //                final Field checksumField = fixMessage.getField(FixConstants.CHECK_SUM_FIELD_NUMBER);
                //                final int numberOfBytesInMessage = checksumField.getStartIndex() - 3 /*10=*/ - bodyLengthField.getEndIndex() - 1/*SOH after body length field*/;
                //                if (bodyLength != numberOfBytesInMessage) {
                //                    log.warn("Body length mismatch, value in message {}, calculated {}. Ignoring message and logging it on debug level", bodyLength, numberOfBytesInMessage);
                //                    log.debug("Ignored message {}", fixMessage);
                //                    return ValidationFailureActions.RELEASE_MESSAGE;
                //                } else {
                return null;
                //                }
            });

    public static final PredicateWithValidator<TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState>> MESSAGE_TYPE_VALIDATOR =
            new PredicateWithValidator<>(ValidationConfig::isShouldCheckBodyLength, (fixMessage, sessionState) -> {
                final CharSequence messageType = fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER);
                for (final char[] possibleMessageType : sessionState.getFixSpec().getMessageTypes()) {
                    if (ArrayUtils.equals(possibleMessageType, messageType)) {
                        return null;
                    }
                }
                if (log.isWarnEnabled()) {
                    log.warn("Invalid message type received, expected one of " + Arrays.deepToString(sessionState.getFixSpec().getMessageTypes()) + ", got " + messageType);
                }
                return (ctx, message, fixMessageObjectPool) -> ctx.writeAndFlush(FixMessageUtils.toRejectMessage(message, RejectReasons.INVALID_MESSAGE_TYPE, FixConstants.MESSAGE_TYPE_FIELD_NUMBER).retain())
                                                                  .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            });

    public static PredicateWithValidator<TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState>> createSendingTimeValidator(Clock clock) {
        return new PredicateWithValidator<>(ValidationConfig::isShouldCheckSendingTime, ((fixMessage, sessionState) -> {
            final long sendingTime = fixMessage.getTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER);
            if (Math.abs(sendingTime - clock.millis()) > FixConstants.SENDING_TIME_ACCURACY_MILLIS) {
                log.warn("Sending time inaccuracy detected. Difference between now and sending time from message is greater than {} millis. Logging out session {}, message will be logged on debug level",
                         FixConstants.SENDING_TIME_ACCURACY_MILLIS, sessionState.getSessionId());
                log.debug("Message with sending time inaccuracy {}", fixMessage);
                return (ctx, message, fixMessageObjectPool) -> {
                    ctx.write(FixMessageUtils.toRejectMessage(message, RejectReasons.SENDING_TIME_ACCURACY_PROBLEM).retain()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    ctx.writeAndFlush(FixMessageUtils.toLogoutMessage(fixMessageObjectPool.getAndRetain(), LogoutTexts.INACCURATE_SENDING_TIME))
                       .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                       .addListener(FixChannelListeners.LOGOUT_SENT)
                       .addListener(ChannelFutureListener.CLOSE);
                };
            } else {
                return null;
            }
        }));
    }

    private static PredicateWithValidator<TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState>> createBeginStringValidator() {
        return new PredicateWithValidator<>(config -> true, (fixMsg, sessionState) -> {
            final CharSequence beginString = fixMsg.getCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER);
            final SessionID sessionId = sessionState.getSessionId();
            if (Comparators.compare(beginString, sessionId.getBeginString()) != 0) {
                log.warn("Unexpected begin string received, expecting {}  but got {}. Logging out session {}", sessionId.getBeginString(), beginString, sessionId);
                return (ctx, message, fixMessageObjectPool) -> ctx.writeAndFlush(FixMessageUtils.toLogoutMessage(message, LogoutTexts.INCORRECT_BEGIN_STRING).retain())
                                                                  .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                                                                  .addListener(FixChannelListeners.LOGOUT_SENT)
                                                                  .addListener(ChannelFutureListener.CLOSE);
            } else {
                return null;
            }
        });
    }

    private static PredicateWithValidator<TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState>> createCompIdValidator() {
        return new PredicateWithValidator<>(ValidationConfig::isShouldCheckSessionIDAfterLogon, (fixMsg, sessionState) -> {
            final SessionID sessionId = sessionState.getSessionId();
            if (!SessionIDUtils.checkCompIDs(fixMsg, sessionId, true)) {
                log.warn("Session id fields in message {} do not match connected session {}", fixMsg, sessionId);
                return (ctx, message, fixMessageObjectPool) -> {
                    ctx.write(FixMessageUtils.toRejectMessage(message, RejectReasons.COMP_ID_PROBLEM).retain()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    ctx.writeAndFlush(FixMessageUtils.toLogoutMessage(fixMessageObjectPool.getAndRetain(), LogoutTexts.INCORRECT_SENDER_OR_TARGET_COMP_ID))
                       .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                       .addListener(FixChannelListeners.LOGOUT_SENT)
                       .addListener(ChannelFutureListener.CLOSE);
                };
            } else {
                return null;
            }
        });
    }
}
