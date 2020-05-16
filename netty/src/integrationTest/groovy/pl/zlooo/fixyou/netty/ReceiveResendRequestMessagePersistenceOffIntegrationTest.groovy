package pl.zlooo.fixyou.netty


import quickfix.Session
import quickfix.field.BeginSeqNo
import quickfix.field.EndSeqNo
import quickfix.field.MsgSeqNum
import quickfix.fixt11.Logon
import quickfix.fixt11.ResendRequest
import quickfix.fixt11.SequenceReset
import spock.lang.Timeout

@Timeout(10)
class ReceiveResendRequestMessagePersistenceOffIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should respond with SequenceReset-Gap Fill when persistence is turned off 8-(not definied)"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        ResendRequest resendRequest = new ResendRequest()
        resendRequest.set(new BeginSeqNo(2))
        resendRequest.set(new EndSeqNo(10))

        when:
        Session.sendToTarget(resendRequest, sessionID)
        pollingConditions.eventually {
            testQuickfixApplication.adminMessagesReceived.size() >= 2
        }

        then:
        nextExpectedInboundSequenceNumber() == 3
        testQuickfixApplication.adminMessagesReceived[0] instanceof Logon
        SequenceReset sequenceResetGapFill = testQuickfixApplication.adminMessagesReceived[1]
        sequenceResetGapFill.getGapFillFlag().value
        sequenceResetGapFill.getHeader().getInt(MsgSeqNum.FIELD) == 2
        sequenceResetGapFill.getNewSeqNo().value == 11
    }
}
