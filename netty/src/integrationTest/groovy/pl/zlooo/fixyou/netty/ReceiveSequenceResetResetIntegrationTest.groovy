package pl.zlooo.fixyou.netty


import quickfix.Session
import quickfix.field.MsgSeqNum
import quickfix.field.NewSeqNo
import quickfix.fixt11.Logon
import quickfix.fixt11.Reject
import quickfix.fixt11.SequenceReset
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

@Timeout(10)
class ReceiveSequenceResetResetIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should set new sequence number when it is grater then existing one 11-a"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        SequenceReset sequenceReset = new SequenceReset()
        sequenceReset.set(new NewSeqNo(666))
        def session = Session.lookupSession(sessionID)
        session.setNextSenderMsgSeqNum(1)

        when:
        Session.sendToTarget(sequenceReset, sessionID)
        pollingConditions.eventually {
            nextExpectedInboundSequenceNumber() > 2
        }

        then:
        nextExpectedInboundSequenceNumber() == 666
        testQuickfixApplication.adminMessagesReceived.size() == 1
        testQuickfixApplication.adminMessagesReceived[0] instanceof Logon
        testFixMessageListener.messagesReceived.isEmpty()
    }

    def "should set new sequence number when it is equal to existing one 11-b"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        SequenceReset sequenceReset = new SequenceReset()
        def nextExpectedSequenceAfterLogon = nextExpectedInboundSequenceNumber()
        sequenceReset.set(new NewSeqNo(nextExpectedSequenceAfterLogon as int))
        def session = Session.lookupSession(sessionID)
        session.setNextSenderMsgSeqNum(1)

        when:
        Session.sendToTarget(sequenceReset, sessionID)
        //TODO figure out a better wait condition
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        then:
        nextExpectedInboundSequenceNumber() == nextExpectedSequenceAfterLogon
        testQuickfixApplication.adminMessagesReceived.size() == 1
        testQuickfixApplication.adminMessagesReceived[0] instanceof Logon
        testFixMessageListener.messagesReceived.isEmpty()
    }

    def "should reject sequence reset - reset mode when new sequence is lower than expected 11-c"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        SequenceReset sequenceReset = new SequenceReset()
        sequenceReset.set(new NewSeqNo(1))
        def session = Session.lookupSession(sessionID)
        session.setNextSenderMsgSeqNum(1)

        when:
        Session.sendToTarget(sequenceReset, sessionID)
        pollingConditions.eventually {
            testQuickfixApplication.adminMessagesReceived.size() >= 2
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
