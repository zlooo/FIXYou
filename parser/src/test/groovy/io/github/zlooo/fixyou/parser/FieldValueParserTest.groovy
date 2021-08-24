package io.github.zlooo.fixyou.parser


import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class FieldValueParserTest extends Specification {

    public static final int CAPACITY = 100

    def "should parse integer"() {
        setup:
        def intHolder = new ValueHolders.IntHolder()

        when:
        FieldValueParser.parseInteger(source, intHolder)

        then:
        intHolder.getValue() == result.abs()
        intHolder.isNegative() == result < 0

        where:
        source                    || result
        buf("-123|")              || -123
        buf("garbage123garbage|") || 123
    }

    def "should parse boolean"() {
        expect:
        FieldValueParser.parseBoolean(source) == expectedValue

        where:
        source           || expectedValue
        buf("Ytest") || true
        buf("N")         || false
    }

    def "should not parse garbage value as boolean"() {
        when:
        FieldValueParser.parseBoolean(source)

        then:
        thrown(IllegalArgumentException)

        where:
        source << [buf("testYtest"), buf("a"), buf("b"), buf("\u0001"), buf("0"), buf("1")]
    }

    def "should parse char"() {
        expect:
        FieldValueParser.parseChar(source) == expectedValue

        where:
        source      || expectedValue
        buf("t") || 't' as char
        buf("e") || 'e' as char
        buf("s") || 's' as char
        buf("t") || 't' as char
        buf("1")  || '1' as char
        buf("2")  || '2' as char
        buf("3")  || '3' as char
    }

    def "should parse double"() {
        setup:
        def byteBuff = Unpooled.buffer(CAPACITY, CAPACITY)
        byteBuff.writeCharSequence(rawValue, StandardCharsets.US_ASCII)
        byteBuff.readerIndex(0)
        def counter = new ValueHolders.IntHolder()
        def decimalHolder = new ValueHolders.DecimalHolder()

        when:
        FieldValueParser.setDoubleValuesFromAsciiByteBuf(byteBuff, counter, decimalHolder)

        then:
        decimalHolder.unscaledValue == expectedValue
        decimalHolder.scale == expectedScale

        where:
        rawValue     | expectedValue | expectedScale
        "123"        | 123           | 0 as short
        "-123"       | -123          | 0 as short
        "123."       | 123           | 0 as short
        "123.45"     | 12345         | 2 as short
        "0000123.45" | 12345         | 2 as short
    }

    private static ByteBuf buf(String value) {
        return Unpooled.wrappedBuffer(value.getBytes(StandardCharsets.US_ASCII))
    }
}
