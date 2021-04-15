package io.github.zlooo.fixyou.session;

import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@FieldNameConstants(asEnum = true)
public class SessionID {

    private final CharSequence beginString;
    private final CharSequence senderCompID;
    private final CharSequence targetCompID;
}
