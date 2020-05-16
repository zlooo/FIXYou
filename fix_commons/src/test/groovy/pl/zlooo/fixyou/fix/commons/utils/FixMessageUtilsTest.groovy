package pl.zlooo.fixyou.fix.commons.utils

import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.fix.commons.TestSpec
import pl.zlooo.fixyou.parser.model.FixMessage
import spock.lang.Ignore
import spock.lang.Specification

@Ignore("test not implemented")
//TODO write this test
class FixMessageUtilsTest extends Specification {

    def "should convert to reject message"() {
        when:
        FixMessageUtils.toRejectMessage(null, 666)

        then:
        assert false: 'test not implemented'
    }

    def "should check if message is administrative"() {
        setup:
        FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = msgType

        expect:
        FixMessageUtils.isAdminMessage(fixMessage) == expectedResult

        where:
        msgType                     | expectedResult
        FixConstants.HEARTBEAT      | true
        FixConstants.TEST_REQUEST   | true
        FixConstants.RESEND_REQUEST | true
        FixConstants.REJECT         | true
        FixConstants.LOGOUT         | true
        FixConstants.LOGON          | true
        FixConstants.SEQUENCE_RESET | true
        "D".toCharArray()           | false
        "8".toCharArray()           | false
        "AJ".toCharArray()          | false
    }
}
