package io.github.zlooo.fixyou.fix.commons.session;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.utils.Comparators;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.session.SessionID;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;

@UtilityClass
public class SessionIDUtils {

    public static void setSessionIdFields(FixMessage fixMessage, SessionID sessionID) {
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, sessionID.getSenderCompID());
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, sessionID.getTargetCompID());
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, sessionID.getBeginString());
    }

    /**
     * Are you sure you need to use this method? SessionID is saved after logon you should use that one whenever possible!!!!
     *
     * @param fixMessage message that will be used to build session id
     * @return build session id
     */
    @Nonnull
    public static SessionID buildSessionID(@Nonnull FixMessage fixMessage, boolean flipIDs) {
        final CharSequence senderCompId = fixMessage.getCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).toString();
        final CharSequence targetCompId = fixMessage.getCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).toString();
        final CharSequence beginString = fixMessage.getCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER).toString();
        if (flipIDs) {
            return new SessionID(beginString, targetCompId, senderCompId);
        } else {
            return new SessionID(beginString, senderCompId, targetCompId);
        }
    }

    public static boolean checkCompIDs(FixMessage fixMsg, SessionID sessionId, boolean flipIDs) {
        final CharSequence senderComparison;
        final CharSequence targetComparison;
        if (flipIDs) {
            senderComparison = sessionId.getTargetCompID();
            targetComparison = sessionId.getSenderCompID();
        } else {
            senderComparison = sessionId.getSenderCompID();
            targetComparison = sessionId.getTargetCompID();
        }
        return Comparators.compare(senderComparison, fixMsg.getCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER)) == 0 &&
               Comparators.compare(targetComparison, fixMsg.getCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER)) == 0;
    }
}
