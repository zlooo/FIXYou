package io.github.zlooo.fixyou.netty.handler.validation;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ValidationOperations {

    static boolean checkOrigSendingTime(FixMessage msg) {
        if (msg.isValueSet(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER)) {
            return msg.getTimestampValue(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER) <= msg.getTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER);
        }
        return true;
    }

    public static boolean isValidLogonMessage(FixMessage fixMessage) {
        return hasValidStandardHeader(fixMessage) && hasRequiredLogonBodyFieldsSet(fixMessage) && hasSequenceNumberResettedIfFlagIsSet(fixMessage);
    }

    private static boolean hasRequiredLogonBodyFieldsSet(FixMessage fixMessage) {
        return FixMessageUtils.hasField(fixMessage, FixConstants.ENCRYPT_METHOD_FIELD_NUMBER) && FixMessageUtils.hasField(fixMessage, FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER) &&
               FixMessageUtils.hasField(fixMessage, FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER);
    }

    private static boolean hasValidStandardHeader(FixMessage fixMessage) {
        return hasValidMinimalSessionIDFields(fixMessage) && hasMessageIntegrityFields(fixMessage) && FixMessageUtils.hasField(fixMessage, FixConstants.SENDING_TIME_FIELD_NUMBER);
    }

    private static boolean hasValidMinimalSessionIDFields(FixMessage fixMessage) {
        return FixMessageUtils.hasField(fixMessage, FixConstants.BEGIN_STRING_FIELD_NUMBER) && FixMessageUtils.hasField(fixMessage, FixConstants.SENDER_COMP_ID_FIELD_NUMBER) &&
               FixMessageUtils.hasField(fixMessage, FixConstants.TARGET_COMP_ID_FIELD_NUMBER);
    }

    private static boolean hasMessageIntegrityFields(FixMessage fixMessage) {
        return FixMessageUtils.hasField(fixMessage, FixConstants.BODY_LENGTH_FIELD_NUMBER) && FixMessageUtils.hasField(fixMessage, FixConstants.MESSAGE_TYPE_FIELD_NUMBER) &&
               FixMessageUtils.hasField(fixMessage, FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER);
    }

    private static boolean hasSequenceNumberResettedIfFlagIsSet(FixMessage fixMessage) {
        return !FixMessageUtils.hasBooleanFieldSet(fixMessage, FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER) || fixMessage.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 1L;
    }
}
