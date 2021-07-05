package io.github.zlooo.fixyou.fix.commons;

import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.session.SessionID;

public interface FixMessageListener {
    void onFixMessage(SessionID sessionID, FixMessage fixMessage);
}
