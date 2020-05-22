package io.github.zlooo.fixyou.commons

import spock.lang.Specification

class ReusableCharArrayTest extends Specification {

    private static final String contence = "some string"
    private ReusableCharArray reusableCharArray = new ReusableCharArray(contence.toCharArray())

    def "should return proper length"() {
        expect:
        reusableCharArray.length() == contence.length()
    }

    def "should return proper char at"() {
        expect:
        reusableCharArray.charAt(index) == contence.charAt(index)

        where:
        index << (0..contence.length() - 1).collect()
    }

    def "should return proper subsequence"() {
        expect:
        contence.substring(start, end).contentEquals(reusableCharArray.subSequence(start, end))

        where:
        start | end
        0     | contence.length()
        1     | contence.length()
        2     | contence.length()
        2     | contence.length() - 1
    }
}
