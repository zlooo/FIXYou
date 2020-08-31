package io.github.zlooo.fixyou.commons.utils

import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

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

    def "should write long to byte buf"() {
        setup:
        def buf = Unpooled.buffer(20)

        when:
        FieldUtils.writeEncoded(value, buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == expectedValue

        where:
        value | expectedValue
        0     | "0"
        321   | "321"
        1     | "1"
        1000  | "1000"
        -1    | "-1"
        -321  | "-321"
    }

    def "should write long to byte buf with min length constraint"() {
        setup:
        def buf = Unpooled.buffer(20)

        when:
        FieldUtils.writeEncoded(value, buf, minLength)

        then:
        buf.toString(StandardCharsets.US_ASCII) == expectedValue

        where:
        value | minLength | expectedValue
        0     | 3         | "000"
        321   | 3         | "321"
        321   | 4         | "0321"
        321   | 5         | "00321"
        1     | 1         | "1"
        1000  | 4         | "1000"
        -1    | 2         | "-1"
        -1    | 3         | "0-1"
        -1    | 4         | "00-1"
        -321  | 4         | "-321"
    }
}
