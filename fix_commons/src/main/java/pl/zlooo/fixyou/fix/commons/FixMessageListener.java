package pl.zlooo.fixyou.fix.commons;

import pl.zlooo.fixyou.parser.model.FixMessage;
import pl.zlooo.fixyou.session.SessionID;

public interface FixMessageListener {
    void onFixMessage(SessionID sessionID, FixMessage fixMessage);
}
