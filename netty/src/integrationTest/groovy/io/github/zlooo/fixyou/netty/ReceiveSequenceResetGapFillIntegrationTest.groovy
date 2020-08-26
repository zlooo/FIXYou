package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.netty.test.framework.FixMessages
import io.github.zlooo.fixyou.netty.test.framework.QuickfixTestUtils
import quickfix.Session
import quickfix.field.GapFillFlag
import quickfix.field.MsgSeqNum
import quickfix.field.NewSeqNo
import quickfix.field.PossDupFlag
import quickfix.fixt11.*
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

@Timeout(30)
class ReceiveSequenceResetGapFillIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {
    def "should send resend when sequence reset - gap fill is received which indicates message gap 10-a"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        SequenceReset sequenceReset = new SequenceReset()
        sequenceReset.set(new GapFillFlag(true))
        def session = Session.lookupSession(sessionID)
        session.nextSenderMsgSeqNum = 6
        sequenceReset.set(new NewSeqNo(10))

        when:
        Session.sendToTarget(sequenceReset, sessionID)
        pollingConditions.eventually {
            nextExpectedInboundSequenceNumber() == 10
        }

        then:
        testQuickfixApplication.adminMessagesReceived.size() == 2
        testQuickfixApplication.adminMessagesReceived[0] instanceof Logon
        ResendRequest resendRequest = testQuickfixApplication.adminMessagesReceived[1]
        resendRequest.getBeginSeqNo().value == 2
        resendRequest.getEndSeqNo().value == 5
        nextExpectedInboundSequenceNumber() == 10
    }

    def "should increase next expected sequence number when sequence reset - gap fill is received without message gap 10-b"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        SequenceReset sequenceReset = new SequenceReset()
        sequenceReset.set(new GapFillFlag(true))
        sequenceReset.set(new NewSeqNo(10))

        when:
        Session.sendToTarget(sequenceReset, sessionID)
        pollingConditions.eventually {
            nextExpectedInboundSequenceNumber() == 10
        }

        then:
        testQuickfixApplication.adminMessagesReceived.size() == 1
        testQuickfixApplication.adminMessagesReceived[0] instanceof Logon
        nextExpectedInboundSequenceNumber() == 10
    }

    def "should ignore sequence reset when message sequence number is less then expected and poss dup flag is set 10-c"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        SequenceReset sequenceReset = new SequenceReset()
        sequenceReset.set(new GapFillFlag(true))
        sequenceReset.set(new NewSeqNo(10))
        QuickfixTestUtils.putSessionIdInfo(sessionID, sequenceReset.getHeader(), false)
        QuickfixTestUtils.putStandardHeaderFields(sequenceReset.getHeader(), 1)
        sequenceReset.getHeader().setBoolean(PossDupFlag.FIELD, true)
        pollingConditions.eventually {
            receivedMessages.size() >= 1
        }

        when:
        sendMessage(channel, sequenceReset.toString())
        //TODO figure out a better wait condition
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        then:
        receivedMessages.size() == 1
        testFixMessageListener.messagesReceived.isEmpty()
        nextExpectedInboundSequenceNumber() == 2
    }

    def "should disconnect session on sequence reset message with sequence number is then expected and poss dup flag is not set 10-d"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        SequenceReset sequenceReset = new SequenceReset()
        sequenceReset.set(new GapFillFlag(true))
        sequenceReset.set(new NewSeqNo(10))
        Session session = Session.lookupSession(sessionID)
        session.setNextSenderMsgSeqNum(1)

        when:
        Session.sendToTarget(sequenceReset, sessionID)
        pollingConditions.eventually {
            testQuickfixApplication.adminMessagesReceived.size() >= 2
            sessionSateListener.sessionState.logoutSent
            !sessionSateListener.sessionState.channel.isActive()
        }

        then:
        testQuickfixApplication.adminMessagesReceived.size() == 2
        testQuickfixApplication.adminMessagesReceived[0] instanceof Logon
        def logout = testQuickfixApplication.adminMessagesReceived[1]
        logout instanceof Logout
        ((Logout) logout).getText().value == "Sequence number is lower than expected"
        testFixMessageListener.messagesReceived.isEmpty()
        nextExpectedInboundSequenceNumber() == 3
        sessionSateListener.sessionState.logoutSent
        sessionSateListener.sessionState.channel != null
        !sessionSateListener.sessionState.channel.isActive()
    }

    def "should reject sequence reset fap fill when new sequence number is lower than expected 10-e"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        SequenceReset sequenceReset = new SequenceReset()
        sequenceReset.set(new GapFillFlag(true))
        sequenceReset.set(new NewSeqNo(1))

        when:
        Session.sendToTarget(sequenceReset, sessionID)
        pollingConditions.eventually {
            testQuickfixApplication.adminMessagesReceived.size() == 2
        }

        then:
        testQuickfixApplication.adminMessagesReceived.size() == 2
        testQuickfixApplication.adminMessagesReceived[0] instanceof Logon
        testQuickfixApplication.adminMessagesReceived[1] instanceof Reject
        Reject reject = testQuickfixApplication.adminMessagesReceived[1]
        reject.getText().value == "Sequence number provided in NewSeqNo(36) field is lower than expected value"
        reject.getRefSeqNum().value == sequenceReset.getHeader().getInt(MsgSeqNum.FIELD)
        testFixMessageListener.messagesReceived.isEmpty()
        nextExpectedInboundSequenceNumber() == 2
    }
}
