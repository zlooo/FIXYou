package io.github.zlooo.fixyou.netty

import quickfix.fixt11.Logon
import quickfix.fixt11.Logout
import spock.lang.Timeout

@Timeout(30)
class InitiateLogoutProcessIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should logout 12"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()

        when:
        FIXYouNetty.logoutSession(engine, fixYouSessionId).sync()
        pollingConditions.eventually {
            testQuickfixApplication.adminMessagesReceived.size() >= 2
            !sessionSateListener.sessionState.connected.get()
        }

        then:
        sessionSateListener.loggedOut
        sessionSateListener.sessionState.logoutSent
        !sessionSateListener.sessionState.connected.get()
        !sessionSateListener.sessionState.channel.isActive()
        testQuickfixApplication.adminMessagesReceived.size() == 2
        testQuickfixApplication.adminMessagesReceived[0] instanceof Logon
        testQuickfixApplication.adminMessagesReceived[1] instanceof Logout
    }
}
