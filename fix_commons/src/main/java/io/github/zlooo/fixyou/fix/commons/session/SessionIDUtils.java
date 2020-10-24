package io.github.zlooo.fixyou.fix.commons.session;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.parser.model.Field;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.session.SessionID;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;

@UtilityClass
public class SessionIDUtils {

    public static void setSessionIdFields(FixMessage fixMessage, SessionID sessionID) {
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).setCharSequenceValue(sessionID.getSenderCompID());
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).setCharSequenceValue(sessionID.getTargetCompID());
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).setCharSequenceValue(sessionID.getBeginString());
    }

    /**
     * Are you sure you need to use this method? SessionID is saved after logon you should use that one whenever possible!!!!
     *
     * @param fixMessage message that will be used to build session id
     * @return build session id
     */
    @Nonnull
    public static SessionID buildSessionID(@Nonnull FixMessage fixMessage, boolean flipIDs) {
        final Field senderCompId = fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER);
        final Field targetCompId = fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER);
        final Field beginString = fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER);
        if (flipIDs) {
            return new SessionID(beginString.getCharArrayValue(), beginString.getLength(), targetCompId.getCharArrayValue(), targetCompId.getLength(), senderCompId.getCharArrayValue(), senderCompId.getLength());
        } else {
            return new SessionID(beginString.getCharArrayValue(), beginString.getLength(), senderCompId.getCharArrayValue(), senderCompId.getLength(), targetCompId.getCharArrayValue(), targetCompId.getLength());
        }
    }

    public static boolean checkCompIDs(FixMessage fixMsg, SessionID sessionId, boolean flipIDs) {
        final CharSequence senderCompId = fixMsg.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).getCharSequenceValue();
        final CharSequence targetCompId = fixMsg.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).getCharSequenceValue();
        final char[] senderComparison;
        final char[] targetComparison;
        if (flipIDs) {
            senderComparison = sessionId.getTargetCompID();
            targetComparison = sessionId.getSenderCompID();
        } else {
            senderComparison = sessionId.getSenderCompID();
            targetComparison = sessionId.getTargetCompID();
        }
        return ArrayUtils.equals(senderComparison, senderCompId) && ArrayUtils.equals(targetComparison, targetCompId);
    }
}
