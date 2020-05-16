package pl.zlooo.fixyou;

import pl.zlooo.fixyou.session.SessionID;

public interface SingleSessionResettable {

    void reset(SessionID sessionID);
}
