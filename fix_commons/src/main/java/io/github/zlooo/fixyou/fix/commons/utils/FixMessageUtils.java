package io.github.zlooo.fixyou.fix.commons.utils;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage;
import io.github.zlooo.fixyou.session.SessionID;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import lombok.experimental.UtilityClass;

import java.util.Arrays;

@UtilityClass
public final class FixMessageUtils {

    static {
        Arrays.sort(FixConstants.ADMIN_MESSAGE_TYPES);
    }

    public static final FixMessage EMPTY_FAKE_MESSAGE = new NotPoolableFixMessage();

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode) {
        final long sourceMessageSequenceNumber = sourceMessage.isValueSet(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) ? sourceMessage.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) : 0;
        sourceMessage.reset();
        if (sourceMessageSequenceNumber > 0) {
            sourceMessage.setLongValue(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, sourceMessageSequenceNumber);
        }
        sourceMessage.setLongValue(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER, rejectReasonCode);
        sourceMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.REJECT);
        return sourceMessage;
    }

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode, long referencedTagNumber) {
        final FixMessage fixMessage = toRejectMessage(sourceMessage, rejectReasonCode);
        fixMessage.setLongValue(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER, referencedTagNumber);
        return fixMessage;
    }

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode, long referencedTagNumber, char[] rejectDescription) {
        final FixMessage fixMessage = toRejectMessage(sourceMessage, rejectReasonCode, referencedTagNumber);
        fixMessage.setCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER, rejectDescription);
        return fixMessage;
    }

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode, char[] rejectDescription) {
        final FixMessage fixMessage = toRejectMessage(sourceMessage, rejectReasonCode);
        fixMessage.setCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER, rejectDescription);
        return fixMessage;
    }

    //TODO maybe create admin and application model, as in 5.0 spec, both generated from dictionary xml
    //TODO move this method out of this class it's supposed to be util
    public static FixMessage toResendRequest(FixMessage fixMessage, long fromInclusive, long toExclusive) {
        fixMessage.reset();
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.RESEND_REQUEST);
        fixMessage.setLongValue(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, fromInclusive);
        fixMessage.setLongValue(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, toExclusive);
        return fixMessage;
    }

    public static FixMessage toLogoutMessage(FixMessage fixMessage, char[] textMessage) {
        fixMessage.reset();
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.LOGOUT);
        if (textMessage != null) {
            fixMessage.setCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER, textMessage);
        }
        return fixMessage;
    }

    public static FixMessage toLogonMessage(FixMessage fixMessage, char[] defaultApplicationVersionId) {
        final long encryptMethod = fixMessage.getLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER);
        final long heartbeatInterval = fixMessage.getLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER);
        fixMessage.reset();
        fixMessage.setLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, encryptMethod);
        fixMessage.setLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, heartbeatInterval);
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.LOGON);
        fixMessage.setCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, defaultApplicationVersionId);
        return fixMessage;
    }

    public static FixMessage toLogonMessage(FixMessage fixMessage, char[] defalutApplicationVersionId, long encryptMethod, long heartbeatInterval, boolean resetMsgSeqFlagSet) {
        fixMessage.reset();
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.LOGON);
        fixMessage.setCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, defalutApplicationVersionId);
        fixMessage.setLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, encryptMethod);
        fixMessage.setLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, heartbeatInterval);
        if (resetMsgSeqFlagSet) {
            fixMessage.setBooleanValue(FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER, true);
        }
        return fixMessage;
    }

    public static FixMessage toLogonMessage(FixMessage fixMessage, char[] defalutApplicationVersionId, long encryptMethod, long heartbeatInterval, boolean resetMsgSeqFlagSet, SessionID sessionID, boolean flipSeddionIDFields) {
        toLogonMessage(fixMessage, defalutApplicationVersionId, encryptMethod, heartbeatInterval, resetMsgSeqFlagSet);
        if (flipSeddionIDFields) {
            fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, sessionID.getTargetCompID());
            fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, sessionID.getSenderCompID());
        } else {
            fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, sessionID.getTargetCompID());
            fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, sessionID.getSenderCompID());
        }
        return fixMessage;
    }

    public static FixMessage toSequenceReset(FixMessage fixMessage, long sequenceNumber, long newSequenceNumber, boolean gapFill) {
        fixMessage.reset();
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.SEQUENCE_RESET);
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, sequenceNumber);
        fixMessage.setLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER, newSequenceNumber);
        fixMessage.setBooleanValue(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER, gapFill);
        return fixMessage;
    }

    public static FixMessage toHeartbeatMessage(FixMessage fixMessage) {
        fixMessage.reset();
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.HEARTBEAT);
        return fixMessage;
    }

    public static FixMessage toHeartbeatMessage(FixMessage fixMessage, CharSequence testReqID) {
        final FixMessage heartbeatMessage = toHeartbeatMessage(fixMessage);
        heartbeatMessage.setCharSequenceValue(FixConstants.TEST_REQ_ID_FIELD_NUMBER, testReqID);
        return heartbeatMessage;
    }

    public static FixMessage toTestRequest(FixMessage fixMessage, char[] testReqID) {
        fixMessage.reset();
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.TEST_REQUEST);
        fixMessage.setCharSequenceValue(FixConstants.TEST_REQ_ID_FIELD_NUMBER, testReqID);
        return fixMessage;
    }

    public static boolean isSequenceReset(FixMessage fixMessage) {
        return ArrayUtils.equals(FixConstants.SEQUENCE_RESET, fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER));
    }

    public static boolean hasBooleanFieldSet(FixMessage fixMessage, int fieldNumber) {
        return fixMessage.isValueSet(fieldNumber) && fixMessage.getBooleanValue(fieldNumber);
    }

    public static boolean hasField(FixMessage message, int fieldNumber) {
        return message.isValueSet(fieldNumber);
    }

    public boolean isAdminMessage(FixMessage fixMessage) {
        final CharSequence messageType = fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER);
        return messageType.length() == 1 && Arrays.binarySearch(FixConstants.ADMIN_MESSAGE_TYPES, messageType.charAt(0)) >= 0;
    }
}
