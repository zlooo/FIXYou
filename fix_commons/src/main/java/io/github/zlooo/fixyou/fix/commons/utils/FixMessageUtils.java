package io.github.zlooo.fixyou.fix.commons.utils;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.model.*;
import io.github.zlooo.fixyou.session.SessionID;
import lombok.experimental.UtilityClass;

import java.util.Arrays;

@UtilityClass
public final class FixMessageUtils {

    static {
        Arrays.sort(FixConstants.ADMIN_MESSAGE_TYPES);
    }

    public static final FixSpec FAKE_SPEC = new FakeFixSpec();
    public static final FixMessage EMPTY_FAKE_MESSAGE = new NotPoolableFixMessage(FixMessageUtils.FAKE_SPEC);

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode) {
        sourceMessage.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER)
                     .setFieldData(sourceMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).getFieldData());
        sourceMessage.resetDataFields(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER);
        sourceMessage.<LongField>getField(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER).setValue(rejectReasonCode);
        sourceMessage.<CharArrayField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setValue(FixConstants.REJECT);
        return sourceMessage;
    }

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode, long referencedTagNumber) {
        final FixMessage fixMessage = toRejectMessage(sourceMessage, rejectReasonCode);
        fixMessage.<LongField>getField(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER).setValue(referencedTagNumber);
        return fixMessage;
    }

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode, long referencedTagNumber, char[] rejectDescription) {
        final FixMessage fixMessage = toRejectMessage(sourceMessage, rejectReasonCode, referencedTagNumber);
        fixMessage.<CharArrayField>getField(FixConstants.TEXT_FIELD_NUMBER).setValue(rejectDescription);
        return fixMessage;
    }

    public static FixMessage toRejectMessage(FixMessage sourceMessage, long rejectReasonCode, char[] rejectDescription) {
        final FixMessage fixMessage = toRejectMessage(sourceMessage, rejectReasonCode);
        fixMessage.<CharArrayField>getField(FixConstants.TEXT_FIELD_NUMBER).setValue(rejectDescription);
        return fixMessage;
    }

    //TODO maybe create admin and application model, as in 5.0 spec, both generated from dictionary xml
    //TODO move this method out of this class it's supposed to be util
    public static FixMessage toResendRequest(FixMessage fixMessage, long fromInclusive, long toExclusive) {
        fixMessage.resetAllDataFields();
        fixMessage.<CharArrayField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setValue(FixConstants.RESEND_REQUEST);
        fixMessage.<LongField>getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).setValue(fromInclusive);
        fixMessage.<LongField>getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).setValue(toExclusive);
        return fixMessage;
    }

    public static FixMessage toLogoutMessage(FixMessage fixMessage, char[] textMessage) {
        fixMessage.resetAllDataFields();
        fixMessage.<CharArrayField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setValue(FixConstants.LOGOUT);
        if (textMessage != null) {
            fixMessage.<CharArrayField>getField(FixConstants.TEXT_FIELD_NUMBER).setValue(textMessage);
        }
        return fixMessage;
    }

    public static FixMessage toLogonMessage(FixMessage fixMessage, char[] defalutApplicationVersionId) {
        fixMessage.resetDataFields(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER); //TODO setup heartbeat handler
        fixMessage.<CharArrayField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setValue(FixConstants.LOGON);
        fixMessage.<CharArrayField>getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).setValue(defalutApplicationVersionId);
        return fixMessage;
    }

    public static FixMessage toLogonMessage(FixMessage fixMessage, char[] defalutApplicationVersionId, long encryptMethod, long heartbeatInterval, boolean resetMsgSeqFlagSet) {
        fixMessage.resetAllDataFields();
        fixMessage.<CharArrayField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setValue(FixConstants.LOGON);
        fixMessage.<CharArrayField>getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).setValue(defalutApplicationVersionId);
        fixMessage.<LongField>getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).setValue(encryptMethod);
        fixMessage.<LongField>getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).setValue(heartbeatInterval);
        if (resetMsgSeqFlagSet) {
            fixMessage.<BooleanField>getField(FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER).setValue(true);
        }
        return fixMessage;
    }

    public static FixMessage toLogonMessage(FixMessage fixMessage, char[] defalutApplicationVersionId, long encryptMethod, long heartbeatInterval, boolean resetMsgSeqFlagSet, SessionID sessionID, boolean flipSeddionIDFields) {
        toLogonMessage(fixMessage, defalutApplicationVersionId, encryptMethod, heartbeatInterval, resetMsgSeqFlagSet);
        if (flipSeddionIDFields) {
            fixMessage.<CharArrayField>getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).setValue(sessionID.getTargetCompID());
            fixMessage.<CharArrayField>getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).setValue(sessionID.getSenderCompID());
        } else {
            fixMessage.<CharArrayField>getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).setValue(sessionID.getTargetCompID());
            fixMessage.<CharArrayField>getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).setValue(sessionID.getSenderCompID());
        }
        return fixMessage;
    }

    public static FixMessage toSequenceReset(FixMessage fixMessage, long sequenceNumber, long newSequenceNumber, boolean gapFill) {
        fixMessage.resetAllDataFields();
        fixMessage.<CharArrayField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setValue(FixConstants.SEQUENCE_RESET);
        fixMessage.<LongField>getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).setValue(sequenceNumber);
        fixMessage.<LongField>getField(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER).setValue(newSequenceNumber);
        fixMessage.<BooleanField>getField(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER).setValue(gapFill);
        return fixMessage;
    }

    public static FixMessage toHeartbeatMessage(FixMessage fixMessage) {
        fixMessage.resetAllDataFields();
        fixMessage.<CharArrayField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setValue(FixConstants.HEARTBEAT);
        return fixMessage;
    }

    public static FixMessage toHeartbeatMessage(FixMessage fixMessage, char[] testReqID) {
        final FixMessage heartbeatMessage = toHeartbeatMessage(fixMessage);
        heartbeatMessage.<CharArrayField>getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).setValue(testReqID);
        return heartbeatMessage;
    }

    public static FixMessage toTestRequest(FixMessage fixMessage, char[] testReqID) {
        fixMessage.resetAllDataFields();
        fixMessage.<CharArrayField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setValue(FixConstants.TEST_REQUEST);
        fixMessage.<CharArrayField>getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).setValue(testReqID);
        return fixMessage;
    }

    public static boolean isSequenceReset(FixMessage fixMessage) {
        return Arrays.equals(FixConstants.SEQUENCE_RESET, fixMessage.<CharArrayField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).getValue());
    }

    public static boolean hasBooleanFieldSet(FixMessage fixMessage, int fieldNumber) {
        final BooleanField field = fixMessage.getField(fieldNumber);
        return field != null && field.isValueSet() && field.getValue();
    }

    public static boolean hasField(FixMessage message, int fieldNumber) {
        final AbstractField field = message.getField(fieldNumber);
        return field != null && field.isValueSet();
    }

    public boolean isAdminMessage(FixMessage fixMessage) {
        final char[] messageType = fixMessage.<CharArrayField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).getValue();
        return messageType.length == 1 && Arrays.binarySearch(FixConstants.ADMIN_MESSAGE_TYPES, messageType[0]) >= 0;
    }
}
