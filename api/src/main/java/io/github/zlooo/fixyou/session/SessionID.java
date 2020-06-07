package io.github.zlooo.fixyou.session;

import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.util.Arrays;

@Value
@FieldNameConstants(asEnum = true)
public class SessionID {

    private final char[] beginString;
    private final char[] senderCompID;
    private final char[] targetCompID;

    public SessionID(char[] beginString, int beginStringLength, char[] senderCompId, int senderCompIdLength, char[] targetCompId, int targetCompIdLength) {
        this.beginString = Arrays.copyOf(beginString, beginStringLength);
        this.senderCompID = Arrays.copyOf(senderCompId, senderCompIdLength);
        this.targetCompID = Arrays.copyOf(targetCompId, targetCompIdLength);
    }

    @Override
    public String toString() {
        return "SessionID{" +
               "beginString=" + String.valueOf(beginString) +
               ", senderCompID=" + String.valueOf(senderCompID) +
               ", targetCompID=" + String.valueOf(targetCompID) +
               '}';
    }
}
