package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.netty.test.framework.FixMessages
import quickfix.Session
import quickfix.field.GapFillFlag
import quickfix.field.NewSeqNo
import quickfix.fixt11.SequenceReset
import spock.lang.Timeout

@Timeout(30)
class ReceiveSequenceResetResetModeIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should set new expected sequence number when sequence reset reset mode is received 11-a"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        Session.sendToTarget(FixMessages.createNewOrderSingle(), sessionID)
        pollingConditions.eventually {
            nextExpectedInboundSequenceNumber() == 3
        }
        SequenceReset sequenceReset = new SequenceReset()
        sequenceReset.set(new NewSeqNo(10))
        sequenceReset.set(new GapFillFlag(false))

        when:
        Session.sendToTarget(sequenceReset, sessionID)
        pollingConditions.eventually {
            testFixMessageListener.messagesReceived.size() == 1
        }

        then:
        testFixMessageListener.messagesReceived.size() == 1
        nextExpectedInboundSequenceNumber() == 10
    }
}
