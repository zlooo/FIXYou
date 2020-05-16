package pl.zlooo.fixyou.netty.test.framework

import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import pl.zlooo.fixyou.session.AbstractSessionState
import pl.zlooo.fixyou.session.SessionStateListener

class TestSessionSateListener implements SessionStateListener {

    public boolean loggedOn = false
    public boolean loggedOut = false
    public NettyHandlerAwareSessionState sessionState

    @Override
    void logOn(AbstractSessionState sessionState) {
        loggedOn = true
        this.sessionState = sessionState
    }

    @Override
    void logOut(AbstractSessionState sessionState) {
        loggedOut = true
        this.sessionState = sessionState
    }
}
