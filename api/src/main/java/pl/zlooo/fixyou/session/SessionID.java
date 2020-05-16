package pl.zlooo.fixyou.session;

import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@FieldNameConstants(asEnum = true)
public class SessionID {

    private char[] beginString;
    private char[] senderCompID;
    private char[] targetCompID;

    @Override
    public String toString() {
        return "SessionID{" +
               "beginString=" + String.valueOf(beginString) +
               ", senderCompID=" + String.valueOf(senderCompID) +
               ", targetCompID=" + String.valueOf(targetCompID) +
               '}';
    }
}
