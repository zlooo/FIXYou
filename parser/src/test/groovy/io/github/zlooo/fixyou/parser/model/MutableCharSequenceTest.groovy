package io.github.zlooo.fixyou.parser.model

import spock.lang.Specification

class MutableCharSequenceTest extends Specification {

    private MutableCharSequence sequence = new MutableCharSequence()

    def "should return length"(){
        setup:
        sequence.setState("testStateLongerThanNeeded".toCharArray())
        sequence.setLength("testState".length())

        expect:
        sequence.length() == "testState".length()
    }
}
