package io.github.zlooo.fixyou.fix.commons.session;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.parser.model.CharArrayField;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.session.SessionID;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;
import java.util.Arrays;

@UtilityClass
public class SessionIDUtils {

    public static void setSessionIdFields(FixMessage fixMessage, SessionID sessionID) {
        fixMessage.<CharArrayField>getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).setValue(sessionID.getSenderCompID());
        fixMessage.<CharArrayField>getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).setValue(sessionID.getTargetCompID());
        fixMessage.<CharArrayField>getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).setValue(sessionID.getBeginString());
    }

    /**
     * Are you sure you need to use this method? SessionID is saved after logon you should use that one whenever possible!!!!
     *
     * @param fixMessage message that will be used to build session id
     * @return build session id
     */
    public static @Nonnull
    SessionID buildSessionID(@Nonnull FixMessage fixMessage, boolean flipIDs) {
        final char[] senderCompId = fixMessage.<CharArrayField>getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).getValue();
        final char[] targetCompId = fixMessage.<CharArrayField>getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).getValue();
        if (flipIDs) {
            return new SessionID(fixMessage.<CharArrayField>getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).getValue(), targetCompId, senderCompId);
        } else {
            return new SessionID(fixMessage.<CharArrayField>getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).getValue(), senderCompId, targetCompId);
        }
    }

    public static boolean checkCompIDs(FixMessage fixMsg, SessionID sessionId, boolean flipIDs) {
        final char[] senderCompId = fixMsg.<CharArrayField>getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).getValue();
        final char[] targetCompId = fixMsg.<CharArrayField>getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).getValue();
        final char[] senderComparison;
        final char[] targetComparison;
        if (flipIDs) {
            senderComparison = sessionId.getTargetCompID();
            targetComparison = sessionId.getSenderCompID();
        } else {
            senderComparison = sessionId.getSenderCompID();
            targetComparison = sessionId.getTargetCompID();
        }
        return Arrays.equals(senderCompId, senderComparison) && Arrays.equals(targetCompId, targetComparison);
    }
}
