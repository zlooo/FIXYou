package io.github.zlooo.fixyou.commons.utils

import spock.lang.Specification

class ComparatorsTest extends Specification {

    def "should compare 2 char sequences"() {
        expect:
        result == Comparators.compare(seq1, seq2)

        where:
        seq1        | seq2         || result
        "abc"       | "abc"        || 0
        "abc"       | "abcd"       || -1
        "abc"       | "def"        || -3
        "def"       | "abc"        || 3
        nccs("abc") | nccs("abc")  || 0
        nccs("abc") | nccs("abcd") || -1
        nccs("abc") | nccs("def")  || -3
        nccs("def") | nccs("abc")  || 3
    }

    private static NotComparableCharSequence nccs(String underlying) {
        return new NotComparableCharSequence(underlying)
    }

    private static final class NotComparableCharSequence implements CharSequence {

        private final String underlying

        NotComparableCharSequence(String underlying) {
            this.underlying = underlying
        }

        @Override
        int length() {
            return underlying.length()
        }

        @Override
        char charAt(int index) {
            return underlying.charAt(index)
        }

        @Override
        CharSequence subSequence(int start, int end) {
            return underlying.subSequence(start, end)
        }
    }
}
