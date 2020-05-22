package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.netty.test.framework.FixMessages
import io.github.zlooo.fixyou.netty.test.framework.QuickfixTestUtils
import quickfix.fixt11.Logon
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

@Timeout(10)
class ReceiveHeartbeatMessageIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should respond with heartbeat to test message 5-a"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        pollingConditions.eventually {
            receivedMessages.size() == 1
        }

        when:
        sendMessage(channel, FixMessages.heartbeat(sessionID, null))
        pollingConditions.eventually {
            receivedMessages.size() >= 1
        } //TODO figure out a better wait condition, this one will be satisfied after sendMessage(channel, FixMessages.logon(sessionID)) request from setup is processed
        Thread.sleep(TimeUnit.SECONDS.toMillis(1))

        then:
        receivedMessages.size() == 1
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
    }
}
