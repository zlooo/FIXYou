package io.github.zlooo.fixyou.netty.test.framework

import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.session.AbstractSessionState
import io.github.zlooo.fixyou.session.SessionStateListener

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
