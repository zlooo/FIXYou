package io.github.zlooo.fixyou.commons.utils

import spock.lang.Specification

class FieldUtilsTest extends Specification {

    def "should write long to char sequence"() {
        expect:
        FieldUtils.toCharSequence(value).charArray == charArrayValue

        where:
        value      | charArrayValue
        1          | "1".toCharArray()
        -1         | "-1".toCharArray()
        10         | "10".toCharArray()
        -10        | "-10".toCharArray()
        9999999999 | "9999999999".toCharArray()
    }

    def "should write long to char sequence and prefix if necessary"() {
        expect:
        FieldUtils.toCharSequenceWithSpecifiedSizeAndDefaultValue(value, 5, '0' as char).charArray == charArrayValue

        where:
        value | charArrayValue
        1     | "00001".toCharArray()
        10    | "00010".toCharArray()
        -1    | "000-1".toCharArray() //yeah those 2 cases do not make a lot of sense, but it's up to the user
        -10   | "00-10".toCharArray()
    }
}
