package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.netty.test.framework.FixMessages
import io.github.zlooo.fixyou.parser.model.CharSequenceField
import org.assertj.core.api.Assertions
import quickfix.field.ClOrdID

class MessageReceiverIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should receive 2 consecutive messages"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def clord1 = UUID.randomUUID().toString()
        def clord2 = UUID.randomUUID().toString()
        def newOrderSingle1 = FixMessages.newOrderSingle(sessionID, 2, { it.set(new ClOrdID(clord1)) })
        def newOrderSingle2 = FixMessages.newOrderSingle(sessionID, 3, { it.set(new ClOrdID(clord2)) })

        when:
        sendMessage(channel, newOrderSingle1 + newOrderSingle2)
        pollingConditions.eventually {
            testFixMessageListener.messagesReceived.size() >= 2
        }

        then:
        Assertions.assertThat(testFixMessageListener.messagesReceived[0].<CharSequenceField> getField(11).value).isEqualToIgnoringNewLines(clord1)
        Assertions.assertThat(testFixMessageListener.messagesReceived[1].<CharSequenceField> getField(11).value).isEqualToIgnoringNewLines(clord2)
    }
}
