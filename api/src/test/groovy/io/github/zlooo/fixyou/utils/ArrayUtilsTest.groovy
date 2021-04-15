package io.github.zlooo.fixyou.utils

import io.github.zlooo.fixyou.utils.ArrayUtils
import org.assertj.core.api.Assertions
import org.assertj.core.data.Index
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
        ['a', 'b', 'c'] as char[] | "abcd"        | false
        ['a', 'b', 'c'] as char[] | "adc"         | false
    }

    def "should find index of"() {
        expect:
        ArrayUtils.indexOf(array, element) == expectedResult

        where:
        array                    | element | expectedResult
        [1, 2, 3, 4, 5] as int[] | 1       | 0
        [1, 2, 3, 4, 5] as int[] | 3       | 2
        [1, 2, 3, 4, 5] as int[] | 5       | 4
        [1, 2, 3, 4, 5] as int[] | -1      | ArrayUtils.NOT_FOUND
    }

    def "should get element at for object array"() {
        setup:
        def array = ["0", "1", "2", "3"] as String[]

        expect:
        ArrayUtils.getElementAt(array, index) == expectedResult

        where:
        index | expectedResult
        0     | "0"
        1     | "1"
        2     | "2"
        3     | "3"
    }

    def "should get element at for int array"() {
        setup:
        def array = [0, 1, 2, 3] as int[]

        expect:
        ArrayUtils.getElementAt(array, index) == expectedResult

        where:
        index | expectedResult
        0     | 0
        1     | 1
        2     | 2
        3     | 3
    }

    def "should get element at for long array"() {
        setup:
        def array = [0, 1, 2, 3] as long[]

        expect:
        ArrayUtils.getElementAt(array, index) == expectedResult

        where:
        index | expectedResult
        0     | 0
        1     | 1
        2     | 2
        3     | 3
    }

    def "should get element at for char array"() {
        setup:
        def array = ["0" as char, "1" as char, "2" as char, "3" as char] as char[]

        expect:
        ArrayUtils.getElementAt(array, index) == expectedResult

        where:
        index | expectedResult
        0     | "0" as char
        1     | "1" as char
        2     | "2" as char
        3     | "3" as char
    }

    def "should put element to object array"() {
        setup:
        def array = new String[4]

        when:
        ArrayUtils.putElementAt(array, index, objectToPut)

        then:
        Assertions.assertThat(array).contains(objectToPut, Index.atIndex(index)).containsOnly(objectToPut, null)

        where:
        index | objectToPut
        0     | "0"
        1     | "1"
        2     | "2"
        3     | "3"
    }

    def "should put element to byte array"() {
        setup:
        def array = new byte[4]

        when:
        ArrayUtils.putElementAt(array, index, objectToPut)

        then:
        Assertions.assertThat(array).contains(objectToPut, Index.atIndex(index)).containsOnly(objectToPut, null)

        where:
        index | objectToPut
        0     | 1 as byte
        1     | 2 as byte
        2     | 3 as byte
        3     | 4 as byte
    }

    def "should put element to int array"() {
        setup:
        def array = new int[4]

        when:
        ArrayUtils.putElementAt(array, index, objectToPut)

        then:
        Assertions.assertThat(array).contains(objectToPut, Index.atIndex(index)).containsOnly(objectToPut, null)

        where:
        index | objectToPut
        0     | 1
        1     | 2
        2     | 3
        3     | 4
    }

    def "should put element to long array"() {
        setup:
        def array = new long[4]

        when:
        ArrayUtils.putElementAt(array, index, objectToPut)

        then:
        Assertions.assertThat(array).contains(objectToPut, Index.atIndex(index)).containsOnly(objectToPut, null)

        where:
        index | objectToPut
        0     | 1L
        1     | 2L
        2     | 3L
        3     | 4L
    }
}
