package pl.zlooo.fixyou.netty

import pl.zlooo.fixyou.netty.test.framework.FixMessages
import pl.zlooo.fixyou.netty.test.framework.QuickfixTestUtils
import quickfix.fixt11.Heartbeat
import quickfix.fixt11.Logon
import quickfix.fixt11.TestRequest
import spock.lang.Timeout

@Timeout(10)
class SendTestRequestIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should send test request after read timeout 6-a"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID, 1, 5))
        pollingConditions.eventually {
            receivedMessages.size() == 1
        }

        when:
        pollingConditions.eventually {
            receivedMessages.size() >= 3
        }

        then:
        receivedMessages.size() == 3
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Heartbeat heartbeat = new Heartbeat()
        heartbeat.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        TestRequest testRequest = new TestRequest()
        testRequest.fromString(receivedMessages[2], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        testRequest.getTestReqID().value == 'test'
    }
}
