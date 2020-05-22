package io.github.zlooo.fixyou.fix.commons.utils


import io.github.zlooo.fixyou.fix.commons.TestSpec
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
        io.github.zlooo.fixyou.parser.model.FixMessage fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(TestSpec.INSTANCE)
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = msgType

        expect:
        FixMessageUtils.isAdminMessage(fixMessage) == expectedResult

        where:
        msgType                     | expectedResult
        io.github.zlooo.fixyou.FixConstants.HEARTBEAT      | true
        io.github.zlooo.fixyou.FixConstants.TEST_REQUEST   | true
        io.github.zlooo.fixyou.FixConstants.RESEND_REQUEST | true
        io.github.zlooo.fixyou.FixConstants.REJECT         | true
        io.github.zlooo.fixyou.FixConstants.LOGOUT         | true
        io.github.zlooo.fixyou.FixConstants.LOGON          | true
        io.github.zlooo.fixyou.FixConstants.SEQUENCE_RESET | true
        "D".toCharArray()           | false
        "8".toCharArray()           | false
        "AJ".toCharArray()          | false
    }
}
