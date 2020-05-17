package pl.zlooo.fixyou.parser.model

import pl.zlooo.fixyou.parser.TestSpec
import spock.lang.Specification

class NotPoolableFixMessageTest extends Specification {

    private NotPoolableFixMessage fixMessage = new NotPoolableFixMessage(TestSpec.INSTANCE)

    def "should return fake ref count"() {
        expect:
        fixMessage.refCnt() == Integer.MIN_VALUE
    }

    def "should do nothing on retain"() {
        when:
        def result = fixMessage.retain()

        then:
        result == fixMessage
        fixMessage.refCnt() == Integer.MIN_VALUE
    }

    def "should do nothing on retain with increment"() {
        when:
        def result = fixMessage.retain(10)

        then:
        result == fixMessage
        fixMessage.refCnt() == Integer.MIN_VALUE
    }

    def "should do nothing on touch"() {
        when:
        def result = fixMessage.touch()

        then:
        result == fixMessage
    }

    def "should do nothing on touch with hint"() {
        when:
        def result = fixMessage.touch("hint")

        then:
        result == fixMessage
    }

    def "should not release"() {
        when:
        def result = fixMessage.release()

        then:
        !result
        fixMessage.refCnt() == Integer.MIN_VALUE
    }

    def "should not release with decrement"() {
        when:
        def result = fixMessage.release(10)

        then:
        !result
        fixMessage.refCnt() == Integer.MIN_VALUE
    }
}
