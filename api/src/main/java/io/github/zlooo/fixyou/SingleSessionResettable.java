package io.github.zlooo.fixyou;

import io.github.zlooo.fixyou.session.SessionID;

public interface SingleSessionResettable {

    void reset(SessionID sessionID);
}
