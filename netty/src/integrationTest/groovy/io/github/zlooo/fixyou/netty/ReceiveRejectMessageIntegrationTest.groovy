package io.github.zlooo.fixyou.netty

import quickfix.Session
import quickfix.field.RefSeqNum
import quickfix.fixt11.Reject
import spock.lang.Timeout

@Timeout(30)
class ReceiveRejectMessageIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should receive valid reject message 7-a"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        Reject reject = new Reject()
        reject.set(new RefSeqNum(1))

        when:
        Session.sendToTarget(reject, sessionID)
        pollingConditions.eventually {
            nextExpectedInboundSequenceNumber() >= 3
        }

        then:
        nextExpectedInboundSequenceNumber() == 3
        testFixMessageListener.messagesReceived.isEmpty()
    }
}
