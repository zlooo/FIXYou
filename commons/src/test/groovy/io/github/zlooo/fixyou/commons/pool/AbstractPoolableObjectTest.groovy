package io.github.zlooo.fixyou.commons.pool

import spock.lang.Specification

class AbstractPoolableObjectTest extends Specification {

    private TestPoolableObject testPoolableObject = new TestPoolableObject()

    def "should return reference count"() {
        expect:
        testPoolableObject.refCnt() == 0
    }

    def "should retain object"() {
        when:
        def result = testPoolableObject.retain()

        then:
        result == testPoolableObject
        result.refCnt() == 1
    }

    def "should retain object with increment"() {
        when:
        def result = testPoolableObject.retain(increment)

        then:
        result == testPoolableObject
        result.refCnt() == increment

        where:
        increment | _
        1         | _
        2         | _
        5         | _
        10        | _
    }

    def "should touch object"() {
        expect:
        testPoolableObject.touch() == testPoolableObject
    }

    def "should touch object with hint"() {
        expect:
        testPoolableObject.touch("hint") == testPoolableObject
    }

    def "should release object"() {
        setup:
        testPoolableObject.retain()

        when:
        def result = testPoolableObject.release()

        then:
        result
        testPoolableObject.refCnt() == 0
    }

    def "should release with decrement"() {
        setup:
        (1..increment).forEach { testPoolableObject.retain() }

        when:
        def result = testPoolableObject.release(decrement)

        then:
        result == expectedResult
        testPoolableObject.refCnt() == increment - decrement

        where:
        increment | decrement | expectedResult
        1         | 1         | true
        2         | 1         | false
        3         | 1         | false
    }

    def "should throw exception when releasing free object"() {
        when:
        def result = testPoolableObject.release()

        then:
        thrown(IllegalStateException)
        !result
    }

    def "should not throw exception when releasing free object"() {
        setup:
        testPoolableObject.exceptionOnReferenceCheckFail = false

        when:
        def result = testPoolableObject.release()

        then:
        !result
    }

    private static class TestPoolableObject extends AbstractPoolableObject {

        private boolean closed = false

        @Override
        void close() {
            closed = true
        }
    }
}
