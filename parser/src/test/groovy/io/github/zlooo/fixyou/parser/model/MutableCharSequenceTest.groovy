package io.github.zlooo.fixyou.parser.model

import spock.lang.Specification

class MutableCharSequenceTest extends Specification {

    private MutableCharSequence sequence = new MutableCharSequence()

    def "should return length"() {
        setup:
        sequence.setState("testStateLongerThanNeeded".toCharArray())
        sequence.setLength("testState".length())

        expect:
        sequence.length() == "testState".length()
    }

    def "should return char at within defined length"() {
        setup:
        def value = "testState"
        sequence.setState(value.toCharArray())
        sequence.setLength(value.length())

        expect:
        sequence.charAt(charIndex) == expectedChar

        where:
        charIndex | expectedChar
        0         | 't' as char
        1         | 'e' as char
        2         | 's' as char
        3         | 't' as char
        4         | 'S' as char
        5         | 't' as char
        6         | 'a' as char
        7         | 't' as char
        8         | 'e' as char
    }

    def "should throw IndexOutOfBoundException when asking for char outside defined length"() {
        setup:
        def value = "testState"
        sequence.setState(value.toCharArray())
        sequence.setLength(value.length() - 1)

        when:
        sequence.charAt(8)

        then:
        thrown(IndexOutOfBoundsException)
    }

    def "should return sub sequence within defined length"() {
        setup:
        def value = "testState"
        sequence.setState(value.toCharArray())
        sequence.setLength(value.length())

        when:
        def result = sequence.subSequence(start, end).toString()

        then:
        result == expectedResult

        where:
        start | end | expectedResult
        0     | 9   | "testState"
        1     | 3   | "es"
        3     | 3   | ""
    }

    def "should throw IndexOutOfBoundException when asking for sub sequence outside defined length"() {
        setup:
        def value = "testState"
        sequence.setState(value.toCharArray())
        sequence.setLength(value.length() - 1)

        when:
        sequence.subSequence(start, end)

        then:
        thrown(IndexOutOfBoundsException)

        where:
        start | end
        1     | 9
        -1    | 6
    }
}
