package io.github.zlooo.fixyou.utils

import io.github.zlooo.fixyou.utils.ArrayUtils
import spock.lang.Specification

class ArrayUtilsTest extends Specification {

    private static char defaultChar

    def "should insert element at given index"() {
        expect:
        ArrayUtils.insertElementAtIndex(array, 'q' as char, index) == expectedArray

        where:
        array                                          | index | expectedArray
        new char[2]                                    | 0     | ['q', defaultChar] as char[]
        ['a', 'b', defaultChar, defaultChar] as char[] | 1     | ['a', 'q', 'b', defaultChar] as char[]
        ['a', 'b', defaultChar, defaultChar] as char[] | 3     | ['a', 'b', defaultChar, 'q'] as char[]
    }

    def "should return max element"() {
        expect:
        ArrayUtils.max(elements) == expectedResult

        where:
        elements                    | expectedResult
        [1, 2, 3, 4] as int[]       | 4
        [5, 33, 8, 34, 2] as int[]  | 34
        [56, 33, 8, 34, 2] as int[] | 56
        [1, 1, 1, 1] as int[]       | 1
        [1] as int[]                | 1
    }

    def "should check if array contains element"() {
        expect:
        ArrayUtils.contains(array, elementToCheck) == result

        where:
        array                          | elementToCheck | result
        ["e1", "e2", "e3"] as String[] | "e2"           | true
        ["e1", "e2", "e3"] as String[] | "e1"           | true
        ["e1", "e2", "e3"] as String[] | "e3"           | true
        ["e1", "e2", "e3"] as String[] | "e4"           | false
    }

    def "should check if array equals char sequence"() {
        expect:
        ArrayUtils.equals(array, charSequence) == result

        where:
        array                     | charSequence || result
        ['a', 'b', 'c'] as char[] | "abc"         | true
        ['a', 'b', 'c'] as char[] | "abcd"         | false
        ['a', 'b', 'c'] as char[] | "adc"         | false
    }
}
