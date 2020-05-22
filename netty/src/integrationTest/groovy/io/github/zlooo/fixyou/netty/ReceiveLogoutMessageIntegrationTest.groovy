package io.github.zlooo.fixyou.netty


import io.github.zlooo.fixyou.netty.test.framework.FixMessages
import io.github.zlooo.fixyou.netty.test.framework.QuickfixTestUtils
import quickfix.Session
import quickfix.fixt11.Logon
import quickfix.fixt11.Logout
import spock.lang.Timeout

@Timeout(10)
class ReceiveLogoutMessageIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should disconnect when logout response is received 13-a"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        pollingConditions.eventually {
            receivedMessages.size() == 1
        }
        FIXYouNetty.logoutSession(engine, fixYouSessionId).sync()
        pollingConditions.eventually {
            receivedMessages.size() == 2
        }

        when:
        sendMessage(channel, FixMessages.logout(sessionID))
        pollingConditions.eventually {
            receivedMessages.size() >= 2
            sessionSateListener.sessionState.logoutSent
            !channel.isActive()
        }

        then:
        sessionSateListener.loggedOut
        sessionSateListener.sessionState.logoutSent
        !sessionSateListener.sessionState.connected.get()
        !sessionSateListener.sessionState.channel.isActive()
        !channel.isActive()
        receivedMessages.size() == 2
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Logout logout = new Logout()
        logout.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
    }

    def "should send logout message when logout is received 13-b"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        Session session = Session.lookupSession(sessionID)

        when:
        session.logout()
        pollingConditions.eventually {
            testQuickfixApplication.adminMessagesReceived.size() >= 2
            sessionSateListener.sessionState.logoutSent
            !sessionSateListener.sessionState.channel.isActive()
        }

        then:
        testQuickfixApplication.adminMessagesReceived.size() == 2
        testQuickfixApplication.adminMessagesReceived[0] instanceof Logon
        testQuickfixApplication.adminMessagesReceived[1] instanceof Logout
        sessionSateListener.loggedOut
        sessionSateListener.sessionState.logoutSent
        !sessionSateListener.sessionState.connected.get()
        sessionSateListener.sessionState.channel != null
        !sessionSateListener.sessionState.channel.isActive()
        sessionHandler().@sessionState.logoutSent
    }
}
