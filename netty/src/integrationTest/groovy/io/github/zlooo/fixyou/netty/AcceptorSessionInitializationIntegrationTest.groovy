package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.netty.handler.SessionHandler
import io.github.zlooo.fixyou.netty.test.framework.FixMessages
import io.github.zlooo.fixyou.netty.test.framework.QuickfixTestUtils
import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions
import quickfix.Initiator
import quickfix.Session
import quickfix.SessionID
import quickfix.SessionStateListener
import quickfix.field.BeginSeqNo
import quickfix.field.EndSeqNo
import quickfix.field.MsgSeqNum
import quickfix.field.SessionRejectReason
import quickfix.fixt11.Logon
import quickfix.fixt11.Logout
import quickfix.fixt11.Reject
import quickfix.fixt11.ResendRequest
import spock.lang.Timeout

import java.nio.charset.StandardCharsets

/**
 * This class contains mostly test cases from fix transport 1.1 specification. They are referenced at the end of each test method name, for example 1S-a-1, which means Ref ID 1S,
 * Condition/Stimulus a, Expected Behaviour 1
 */
@Timeout(10)
class AcceptorSessionInitializationIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should initialize session test case 1S-a-1"() {
        when:
        initiator.start()
        pollingConditions.eventually {
            !testQuickfixApplication.loggedOnSessions.isEmpty()
        }

        then:
        Assertions.assertThat(testQuickfixApplication.loggedOnSessions).containsOnly(sessionID)
        sessionSateListener.loggedOn
        sessionSateListener.sessionState.connected.get()
        sessionSateListener.sessionState.logonSent
        !sessionSateListener.sessionState.logoutSent
        sessionSateListener.sessionState.channel != null
    }

    def "should initialize session with gap detected test case 1S-a-2"() {
        setup:
        initiator.start()
        def initiatorSession = Session.lookupSession(sessionID)
        initiatorSession.logout()
        while (!initiatorSession.isLogoutReceived() && initiatorSession.logonSent) {
            Thread.sleep(500)
        }
        initiatorSession.nextSenderMsgSeqNum = 10

        when:
        initiatorSession.logon()
        pollingConditions.eventually {
            testQuickfixApplication.adminMessagesReceived.size() >= 2
        }

        then:
        testQuickfixApplication.adminMessagesReceived.size() == 2
        def message = testQuickfixApplication.adminMessagesReceived[0]
        message instanceof Logon
        def message2 = testQuickfixApplication.adminMessagesReceived[1]
        message2 instanceof ResendRequest
        ((ResendRequest) message2).get(new BeginSeqNo()).value == 1
        ((ResendRequest) message2).get(new EndSeqNo()).value == 9
        sessionSateListener.loggedOn
        sessionSateListener.sessionState.connected.get()
        sessionSateListener.sessionState.logonSent
        !sessionSateListener.sessionState.logoutSent
        sessionSateListener.sessionState.channel != null
    }

    def "should disconnect when second logon attempt on same session is done test case 1S-b-2"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        SessionID secondSessionID = new SessionID("FIXT.1.1", targetCompId, senderCompId, qualifier)
        Initiator secondInitiator = QuickfixTestUtils.setupInitiator(acceptorPort, secondSessionID, testQuickfixApplication)
        secondInitiator.start()
        def session2 = Session.lookupSession(secondSessionID)
        def sessionStateListener = new TestSessionStateListener()
        session2.addStateListener(sessionStateListener)

        when:
        pollingConditions.eventually {
            sessionStateListener.disconnectHappened
        }

        then:
        !sessionStateListener.connected
        sessionStateListener.disconnectHappened
        session2.getRemoteAddress() == null //this means no tcp connection is established, meaning acceptor closed connection
        sessionSateListener.loggedOn
        !sessionSateListener.loggedOut
        sessionSateListener.sessionState.connected.get()
        sessionSateListener.sessionState.logonSent
        !sessionSateListener.sessionState.logoutSent
        sessionSateListener.sessionState.channel != null
        sessionSateListener.sessionState.channel.isActive()
    }

    def "should disconnect when logon attempt on unknown session is done test case 1S-c-2"() {
        setup:
        SessionID sessionIdForUnknownSession = new SessionID("FIXT.1.1", senderCompId + "NotConfigured", targetCompId)
        Initiator unknownSessionInitiator = QuickfixTestUtils.setupInitiator(acceptorPort, sessionIdForUnknownSession, testQuickfixApplication)
        unknownSessionInitiator.start()
        def session2 = Session.lookupSession(sessionIdForUnknownSession)
        def sessionStateListener = new TestSessionStateListener()
        session2.addStateListener(sessionStateListener)

        when:
        pollingConditions.eventually {
            sessionStateListener.disconnectHappened
        }

        then:
        !sessionStateListener.connected
        sessionStateListener.disconnectHappened
        session2.getRemoteAddress() == null //this means no tcp connection is established, meaning acceptor closed connection
        !sessionSateListener.loggedOn
        sessionSateListener.sessionState == null
    }

    def "should logout when invalid logon message is sent test case 1S-d-2-4"() {
        setup:
        def channel = connect()

        when:
        channel.
                writeAndFlush(Unpooled.
                        wrappedBuffer(
                                "8=${sessionID.beginString}\u00019=55\u000135=A\u000149=${sessionID.senderCompID}\u000156=${sessionID.targetCompID}\u000110=087\u0001".getBytes(
                                        StandardCharsets.US_ASCII))).
                sync()
        pollingConditions.eventually {
            receivedMessages.size() >= 2
        }

        then:
        !channel.isActive()
        !sessionSateListener.loggedOn
        sessionSateListener.sessionState == null
        receivedMessages.size() == 2
        Reject reject = new Reject()
        reject.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        reject.getText().value == "Invalid logon message"
        reject.getSessionRejectReason().value == SessionRejectReason.OTHER
        Logout logout = new Logout()
        logout.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        logout.getText().value == "Invalid logon message"
    }

    def "should disconnect if first message is not a logon tests case 1S-e(?)-2"() {
        setup:
        def channel = connect()

        when:
        channel.
                writeAndFlush(Unpooled.
                        wrappedBuffer(
                                "8=${sessionID.beginString}\u00019=55\u000135=1\u000149=${sessionID.senderCompID}\u000156=${sessionID.targetCompID}\u000110=087\u0001".getBytes(
                                        StandardCharsets.US_ASCII))).
                sync()
        pollingConditions.eventually {
            !channel.isActive()
        }

        then:
        !channel.isActive()
        !sessionSateListener.loggedOn
        sessionSateListener.sessionState == null
    }

    def "should reset session when logon with reset sequence number flag is sent"() {
        setup:
        sessionHandler().nextExpectedInboundSequenceNumber = 666L
        sessionHandler().lastOutboundSequenceNumber = 666L
        def channel = connect()

        when:
        sendMessage(channel, FixMessages.logon(sessionID, 1, 30, true)).sync()
        pollingConditions.eventually {
            !receivedMessages.empty
        }

        then:
        sessionSateListener.loggedOn
        sessionSateListener.sessionState.connected.get()
        sessionSateListener.sessionState.logonSent
        !sessionSateListener.sessionState.logoutSent
        sessionSateListener.sessionState.channel != null
        sessionHandler().lastOutboundSequenceNumber == SessionHandler.DEFAULT_OUTBOUND_SEQUENCE_NUMBER + 1 //+1 because outgoing logon will bump that
        sessionHandler().nextExpectedInboundSequenceNumber == SessionHandler.DEFAULT_NEXT_EXPECTED_INBOUND_SEQUENCE_NUMBER + 1 //+1 because incoming logon message will bump that
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        logon.getResetSeqNumFlag().value
        logon.getHeader().getInt(MsgSeqNum.FIELD) == 1
    }

    def "should disconnect when logon with reset sequence number flag is sent but has sequence number greater than 1"() {
        setup:
        sessionHandler().nextExpectedInboundSequenceNumber = 666L
        sessionHandler().lastOutboundSequenceNumber = 666L
        def channel = connect()

        when:
        sendMessage(channel, FixMessages.logon(sessionID, 2, 30, true)).sync()
        pollingConditions.eventually {
            !channel.isActive()
        }

        then:
        !sessionSateListener.loggedOn
        sessionSateListener.sessionState == null
        sessionHandler().lastOutboundSequenceNumber == 666L + 2 //+2 because for now I decided that FIXYou will send reject and logout before disconnect in case where 141=Y and 34!=1
        sessionHandler().nextExpectedInboundSequenceNumber == 666L
        Reject reject = new Reject()
        reject.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        reject.getText().value == "Invalid logon message"
        reject.getSessionRejectReason().value == SessionRejectReason.OTHER
        Logout logout = new Logout()
        logout.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        logout.getText().value == "Invalid logon message"
    }

    private static final class TestSessionStateListener implements SessionStateListener {

        private boolean connected
        private boolean disconnectHappened

        @Override
        void onConnect() {
            connected = true
        }

        @Override
        void onDisconnect() {
            connected = false
            disconnectHappened = true
        }

        @Override
        void onLogon() {

        }

        @Override
        void onLogout() {

        }

        @Override
        void onReset() {

        }

        @Override
        void onRefresh() {

        }

        @Override
        void onMissedHeartBeat() {

        }

        @Override
        void onHeartBeatTimeout() {

        }
    }
}
