package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.netty.test.framework.FixMessages
import org.assertj.core.api.Assertions
import quickfix.field.ClOrdID
import quickfix.field.MsgType

class SendMessageIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should send fix messages normally"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        def clordid1 = UUID.randomUUID()
        def clordid2 = UUID.randomUUID()
        def clordid3 = UUID.randomUUID()

        when:
        FIXYouNetty.sendMessage(FixMessages.createFIXYouNewOrderSingle(clordid1), fixYouSessionId, engine)
        FIXYouNetty.sendMessage(FixMessages.createFIXYouNewOrderSingle(clordid2), fixYouSessionId, engine)
        FIXYouNetty.sendMessage(FixMessages.createFIXYouNewOrderSingle(clordid3), fixYouSessionId, engine)
        pollingConditions.eventually {
            testQuickfixApplication.messagesReceived.size() >= 3
        }

        then:
        Assertions.assertThat(testQuickfixApplication.messagesReceived.collect { it.getHeader().getField(MsgType.FIELD).value }).containsOnly("D")
        Assertions.assertThat(testQuickfixApplication.messagesReceived.collect { it.getField(ClOrdID.FIELD).value }).containsExactly(clordid1.toString(), clordid2.toString(), clordid3.toString())
    }
}
