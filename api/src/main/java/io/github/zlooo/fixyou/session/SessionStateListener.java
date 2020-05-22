package io.github.zlooo.fixyou.session;

public interface SessionStateListener {
    void logOn(AbstractSessionState sessionState);

    void logOut(AbstractSessionState sessionState);
}
