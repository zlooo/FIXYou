package pl.zlooo.fixyou.netty

import pl.zlooo.fixyou.netty.FIXYouNettyInitiator.MultipleVoidFuturesWrapper
import spock.lang.Specification

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class MultipleVoidFuturesWrapperTest extends Specification {

    private Future future1 = Mock()
    private Future future2 = Mock()
    private MultipleVoidFuturesWrapper multipleVoidFuturesWrapper = new MultipleVoidFuturesWrapper()

    void setup() {
        multipleVoidFuturesWrapper.futures.add(future1)
        multipleVoidFuturesWrapper.futures.add(future2)
    }

    def "should cancel all futures"() {
        when:
        def result = multipleVoidFuturesWrapper.cancel(true)

        then:
        result == expectedResult
        1 * future1.cancel(true) >> future1Cancel
        1 * future2.cancel(true) >> future2Cancel
        0 * _

        where:
        future1Cancel | future2Cancel | expectedResult
        true          | true          | true
        true          | false         | false
        false         | true          | false
    }

    def "should check if futures are canceled"() {
        when:
        def result = multipleVoidFuturesWrapper.isCancelled()

        then:
        result == expectedResult
        1 * future1.isCancelled() >> future1Cancel
        1 * future2.isCancelled() >> future2Cancel
        0 * _

        where:
        future1Cancel | future2Cancel | expectedResult
        true          | true          | true
        true          | false         | false
        false         | true          | false
    }

    def "should check if futures are done"() {
        when:
        def result = multipleVoidFuturesWrapper.isDone()

        then:
        result == expectedResult
        1 * future1.isDone() >> future1Done
        1 * future2.isDone() >> future2Done
        0 * _

        where:
        future1Done | future2Done | expectedResult
        true        | true        | true
        true        | false       | false
        false       | true        | false
    }

    def "should timeout waiting for future to be completed"() {
        when:
        multipleVoidFuturesWrapper.get(10, TimeUnit.MILLISECONDS)

        then:
        thrown(TimeoutException)
        _ * future1.isDone() >> false
        _ * future2.isDone() >> false
        0 * _
    }

    def "should not timeout waiting for future to be completed"() {
        when:
        def result = multipleVoidFuturesWrapper.get(10, TimeUnit.MILLISECONDS)

        then:
        result == null
        _ * future1.isDone() >> false >> true
        _ * future2.isDone() >> false >> true
        0 * _
    }

    def "should wait till future is done"() {
        when:
        def result = multipleVoidFuturesWrapper.get()

        then:
        result == null
        2 * future1.isDone() >> false >> true
        2 * future2.isDone() >> false >> true
        0 * _
    }
}
