package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.netty.test.framework.FixMessages
import io.github.zlooo.fixyou.netty.test.framework.QuickfixTestUtils
import quickfix.fixt11.Heartbeat
import quickfix.fixt11.Logon
import spock.lang.Timeout

@Timeout(10)
class SendHeartbeatMessageIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should send heartbeat after inactivity period 4-a"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID, 1, 5))
        pollingConditions.eventually {
            receivedMessages.size() == 1
        }

        when:
        pollingConditions.eventually {
            receivedMessages.size() >= 2
        }

        then:
        receivedMessages.size() == 2
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Heartbeat heartbeat = new Heartbeat()
        heartbeat.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
    }

    def "should respond with heartbeat to test message 4-b"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        pollingConditions.eventually {
            receivedMessages.size() == 1
        }
        sendMessage(channel, FixMessages.testRequest(sessionID, "request1"))

        when:
        pollingConditions.eventually {
            receivedMessages.size() >= 2
        }

        then:
        receivedMessages.size() == 2
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Heartbeat heartbeat = new Heartbeat()
        heartbeat.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        heartbeat.getTestReqID().value == "request1"
    }
}
