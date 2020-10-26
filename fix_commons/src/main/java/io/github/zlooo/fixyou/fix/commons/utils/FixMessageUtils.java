package io.github.zlooo.fixyou.fix.commons.utils;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.parser.model.Field;
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

    public static final FixMessage EMPTY_FAKE_MESSAGE = new NotPoolableFixMessage(null);

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode) {
        final long sourceMessageSequenceNumber = sourceMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).getLongValue();
        sourceMessage.resetAllDataFieldsAndReleaseByteSource();
        if (sourceMessageSequenceNumber > 0) {
            sourceMessage.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER).setLongValue(sourceMessageSequenceNumber);
        }
        sourceMessage.getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).setLongValue(rejectReasonCode);
        sourceMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setCharSequenceValue(FixConstants.REJECT);
        return sourceMessage;
    }

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode, long referencedTagNumber) {
        final FixMessage fixMessage = toRejectMessage(sourceMessage, rejectReasonCode);
        fixMessage.getField(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER).setLongValue(referencedTagNumber);
        return fixMessage;
    }

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode, long referencedTagNumber, char[] rejectDescription) {
        final FixMessage fixMessage = toRejectMessage(sourceMessage, rejectReasonCode, referencedTagNumber);
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).setCharSequenceValue(rejectDescription);
        return fixMessage;
    }

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode, char[] rejectDescription) {
        final FixMessage fixMessage = toRejectMessage(sourceMessage, rejectReasonCode);
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).setCharSequenceValue(rejectDescription);
        return fixMessage;
    }

    //TODO maybe create admin and application model, as in 5.0 spec, both generated from dictionary xml
    //TODO move this method out of this class it's supposed to be util
    public static FixMessage toResendRequest(FixMessage fixMessage, long fromInclusive, long toExclusive) {
        fixMessage.resetAllDataFieldsAndReleaseByteSource();
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setCharSequenceValue(FixConstants.RESEND_REQUEST);
        fixMessage.getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).setLongValue(fromInclusive);
        fixMessage.getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).setLongValue(toExclusive);
        return fixMessage;
    }

    public static FixMessage toLogoutMessage(FixMessage fixMessage, char[] textMessage) {
        fixMessage.resetAllDataFieldsAndReleaseByteSource();
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setCharSequenceValue(FixConstants.LOGOUT);
        if (textMessage != null) {
            fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).setCharSequenceValue(textMessage);
        }
        return fixMessage;
    }

    public static FixMessage toLogonMessage(FixMessage fixMessage, char[] defaultApplicationVersionId) {
        final long encryptMethod = fixMessage.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).getLongValue();
        final long heartbeatInterval = fixMessage.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).getLongValue();
        fixMessage.resetAllDataFieldsAndReleaseByteSource();
        fixMessage.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).setLongValue(encryptMethod);
        fixMessage.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).setLongValue(heartbeatInterval);
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setCharSequenceValue(FixConstants.LOGON);
        fixMessage.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).setCharSequenceValue(defaultApplicationVersionId);
        return fixMessage;
    }

    public static FixMessage toLogonMessage(FixMessage fixMessage, char[] defalutApplicationVersionId, long encryptMethod, long heartbeatInterval, boolean resetMsgSeqFlagSet) {
        fixMessage.resetAllDataFieldsAndReleaseByteSource();
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setCharSequenceValue(FixConstants.LOGON);
        fixMessage.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).setCharSequenceValue(defalutApplicationVersionId);
        fixMessage.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).setLongValue(encryptMethod);
        fixMessage.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).setLongValue(heartbeatInterval);
        if (resetMsgSeqFlagSet) {
            fixMessage.getField(FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER).setBooleanValue(true);
        }
        return fixMessage;
    }

    public static FixMessage toLogonMessage(FixMessage fixMessage, char[] defalutApplicationVersionId, long encryptMethod, long heartbeatInterval, boolean resetMsgSeqFlagSet, SessionID sessionID, boolean flipSeddionIDFields) {
        toLogonMessage(fixMessage, defalutApplicationVersionId, encryptMethod, heartbeatInterval, resetMsgSeqFlagSet);
        if (flipSeddionIDFields) {
            fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).setCharSequenceValue(sessionID.getTargetCompID());
            fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).setCharSequenceValue(sessionID.getSenderCompID());
        } else {
            fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).setCharSequenceValue(sessionID.getTargetCompID());
            fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).setCharSequenceValue(sessionID.getSenderCompID());
        }
        return fixMessage;
    }

    public static FixMessage toSequenceReset(FixMessage fixMessage, long sequenceNumber, long newSequenceNumber, boolean gapFill) {
        fixMessage.resetAllDataFieldsAndReleaseByteSource();
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setCharSequenceValue(FixConstants.SEQUENCE_RESET);
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).setLongValue(sequenceNumber);
        fixMessage.getField(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).setLongValue(newSequenceNumber);
        fixMessage.getField(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER).setBooleanValue(gapFill);
        return fixMessage;
    }

    public static FixMessage toHeartbeatMessage(FixMessage fixMessage) {
        fixMessage.resetAllDataFieldsAndReleaseByteSource();
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setCharSequenceValue(FixConstants.HEARTBEAT);
        return fixMessage;
    }

    public static FixMessage toHeartbeatMessage(FixMessage fixMessage, char[] testReqID, int testReqIDLength) {
        final FixMessage heartbeatMessage = toHeartbeatMessage(fixMessage);
        heartbeatMessage.getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).setCharSequenceValue(testReqID, testReqIDLength);
        return heartbeatMessage;
    }

    public static FixMessage toTestRequest(FixMessage fixMessage, char[] testReqID) {
        fixMessage.resetAllDataFieldsAndReleaseByteSource();
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setCharSequenceValue(FixConstants.TEST_REQUEST);
        fixMessage.getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).setCharSequenceValue(testReqID);
        return fixMessage;
    }

    public static boolean isSequenceReset(FixMessage fixMessage) {
        return ArrayUtils.equals(FixConstants.SEQUENCE_RESET, fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).getCharSequenceValue());
    }

    public static boolean hasBooleanFieldSet(FixMessage fixMessage, int fieldNumber) {
        final Field field = fixMessage.getField(fieldNumber);
        return field != null && field.isValueSet() && field.getBooleanValue();
    }

    public static boolean hasField(FixMessage message, int fieldNumber) {
        final Field field = message.getField(fieldNumber);
        return field != null && field.isValueSet();
    }

    public boolean isAdminMessage(FixMessage fixMessage) {
        final CharSequence messageType = fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).getCharSequenceValue();
        return messageType.length() == 1 && Arrays.binarySearch(FixConstants.ADMIN_MESSAGE_TYPES, messageType.charAt(0)) >= 0;
    }
}
