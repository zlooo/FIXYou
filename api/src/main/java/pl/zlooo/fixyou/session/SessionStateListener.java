package pl.zlooo.fixyou.session;

public interface SessionStateListener {
    void logOn(AbstractSessionState sessionState);

    void logOut(AbstractSessionState sessionState);
}
