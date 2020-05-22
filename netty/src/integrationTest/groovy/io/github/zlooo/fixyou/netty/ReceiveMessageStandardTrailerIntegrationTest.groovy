package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.netty.test.framework.FixMessages
import quickfix.field.ClOrdID
import spock.lang.Ignore
import spock.lang.Timeout

import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

@Ignore("for now checksum validation is not implemented, will have to add that in the future")
@Timeout(10)
class ReceiveMessageStandardTrailerIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should process message with correct checksum 3-a,d"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def expectedClordId
        def message = FixMessages.newOrderSingle(sessionID, 2, { newOrderSingle ->
            expectedClordId = newOrderSingle.getClOrdID().getValue().toCharArray()
        })

        when:
        sendMessage(channel, message)
        pollingConditions.eventually {
            testFixMessageListener.messagesReceived.size() >= 1
        }

        then:
        testFixMessageListener.messagesReceived.size() == 1
        nextExpectedInboundSequenceNumber() == 3
    }

    def "should ignore message with invalid checksum 3-b"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def message = FixMessages.newOrderSingle(sessionID, 2).replaceAll("10=\\d+\\x01", "10=666\u0001")

        when:
        sendMessage(channel, message)
        pollingConditions.eventually {
            receivedMessages.size() >= 1
        } //TODO figure out a better wait condition, this one will be satisfied after sendMessage(channel, FixMessages.logon(sessionID)) request from setup is processed
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        then:
        nextExpectedInboundSequenceNumber() == 2
    }

    def "should ignore message which checksum is not followed by SOH 3-e"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def message = FixMessages.newOrderSingle(sessionID, 2)
        def expectedClordId
        def message2 = FixMessages.newOrderSingle(sessionID, 2, { newOrderSingle ->
            expectedClordId = newOrderSingle.getClOrdID().getValue().toCharArray()
        })

        when:
        sendMessage(channel, message.substring(0, message.length() - 1))
        pollingConditions.eventually {
            receivedMessages.size() >= 1
        } //TODO figure out a better wait condition, this one will be satisfied after sendMessage(channel, FixMessages.logon(sessionID)) request from setup is processed
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        then:
        nextExpectedInboundSequenceNumber() == 2

        when:
        sendMessage(channel, message2)
        pollingConditions.eventually {
            testFixMessageListener.messagesReceived.size() >= 1
        }

        then:
        testFixMessageListener.messagesReceived.size() == 1
        testFixMessageListener.messagesReceived[0].getField(ClOrdID.FIELD).value == expectedClordId
        nextExpectedInboundSequenceNumber() == 3
    }

    def "should ignore message which checksum not 3 chars long 3-e"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def message = FixMessages.newOrderSingle(sessionID, 2)
        def matcher = Pattern.compile(".*10=(\\d+)\\x01").matcher(message)
        matcher.matches()
        message = message.replaceAll("10=\\d+\\x01", "10=00${matcher.group(1)}\u0001")
        def expectedClordId
        def message2 = FixMessages.newOrderSingle(sessionID, 2, { newOrderSingle ->
            expectedClordId = newOrderSingle.getClOrdID().getValue().toCharArray()
        })

        when:
        sendMessage(channel, message)
        pollingConditions.eventually {
            receivedMessages.size() >= 1
        } //TODO figure out a better wait condition, this one will be satisfied after sendMessage(channel, FixMessages.logon(sessionID)) request from setup is processed
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        then:
        nextExpectedInboundSequenceNumber() == 2

        when:
        sendMessage(channel, message2)
        pollingConditions.eventually {
            testFixMessageListener.messagesReceived.size() >= 1
        }

        then:
        testFixMessageListener.messagesReceived.size() == 1
        testFixMessageListener.messagesReceived[0].getField(ClOrdID.FIELD).value == expectedClordId
        nextExpectedInboundSequenceNumber() == 3
    }

    def "should ignore message which checksum not last tag 3-e"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def message = FixMessages.newOrderSingle(sessionID, 2,)
        def matcher = Pattern.compile(".*(10=\\d+\\x01)").matcher(message)
        matcher.matches()
        message = message.replaceAll("10=\\d+\\x01", "").replaceAll("60=", matcher.group(1) + "60=")
        def expectedClordId
        def message2 = FixMessages.newOrderSingle(sessionID, 2, { newOrderSingle ->
            expectedClordId = newOrderSingle.getClOrdID().getValue().toCharArray()
        })

        when:
        sendMessage(channel, message)
        pollingConditions.eventually {
            receivedMessages.size() >= 1
        } //TODO figure out a better wait condition, this one will be satisfied after sendMessage(channel, FixMessages.logon(sessionID)) request from setup is processed
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        then:
        nextExpectedInboundSequenceNumber() == 2

        when:
        sendMessage(channel, message2)
        pollingConditions.eventually {
            testFixMessageListener.messagesReceived.size() >= 1
        }

        then:
        testFixMessageListener.messagesReceived.size() == 1
        testFixMessageListener.messagesReceived[0].getField(ClOrdID.FIELD).value == expectedClordId
        nextExpectedInboundSequenceNumber() == 3
    }
}
