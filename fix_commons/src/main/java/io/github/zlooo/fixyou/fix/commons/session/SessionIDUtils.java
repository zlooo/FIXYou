package io.github.zlooo.fixyou.fix.commons.session;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.parser.model.CharSequenceField;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.session.SessionID;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import lombok.experimental.UtilityClass;

import javax.annotation.Nonnull;

@UtilityClass
public class SessionIDUtils {

    public static void setSessionIdFields(FixMessage fixMessage, SessionID sessionID) {
        fixMessage.<CharSequenceField>getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).setValue(sessionID.getSenderCompID());
        fixMessage.<CharSequenceField>getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).setValue(sessionID.getTargetCompID());
        fixMessage.<CharSequenceField>getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).setValue(sessionID.getBeginString());
    }

    /**
     * Are you sure you need to use this method? SessionID is saved after logon you should use that one whenever possible!!!!
     *
     * @param fixMessage message that will be used to build session id
     * @return build session id
     */
    @Nonnull
    public static SessionID buildSessionID(@Nonnull FixMessage fixMessage, boolean flipIDs) {
        final CharSequenceField senderCompId = fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER);
        senderCompId.getValue(); //so that value is parsed
        final CharSequenceField targetCompId = fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER);
        targetCompId.getValue();
        final CharSequenceField beginString = fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER);
        beginString.getValue();
        if (flipIDs) {
            return new SessionID(beginString.getUnderlyingValue(), beginString.getLength(), targetCompId.getUnderlyingValue(), targetCompId.getLength(), senderCompId.getUnderlyingValue(), senderCompId.getLength());
        } else {
            return new SessionID(beginString.getUnderlyingValue(), beginString.getLength(), senderCompId.getUnderlyingValue(), senderCompId.getLength(), targetCompId.getUnderlyingValue(), targetCompId.getLength());
        }
    }

    public static boolean checkCompIDs(FixMessage fixMsg, SessionID sessionId, boolean flipIDs) {
        final CharSequence senderCompId = fixMsg.<CharSequenceField>getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).getValue();
        final CharSequence targetCompId = fixMsg.<CharSequenceField>getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).getValue();
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
