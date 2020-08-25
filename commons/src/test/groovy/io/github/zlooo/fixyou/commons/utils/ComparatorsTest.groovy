package io.github.zlooo.fixyou.commons.utils

import spock.lang.Specification

class ComparatorsTest extends Specification {

    def "should compare 2 char sequences"() {
        expect:
        result == Comparators.compare(seq1, seq2)

        where:
        seq1  | seq2  || result
        "abc" | "abc" || 0
        "abc" | "abcd" || -1
        "abc" | "def" || -3
        "def" | "abc" || 3
    }
}
