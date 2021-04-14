package io.github.zlooo.fixyou.parser

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class FieldValueParserTest extends Specification {

    public static final int CAPACITY = 100

    def "should parse integer"() {
        expect:
        FieldValueParser.parseInteger(source, srcIndex, endIndicator, false) == result

        where:
        source                     | srcIndex | endIndicator || result
        comp("-123|")              | 0        | 124 as byte  || -123
        comp("garbage123garbage|") | 0        | 124 as byte  || 123
    }

    def "should parse boolean"() {
        expect:
        FieldValueParser.parseBoolean(source, srcIndex) == expectedValue

        where:
        source            | srcIndex || expectedValue
        comp("testYtest") | 4        || true
        comp("N")         | 0        || false
    }

    def "should not parse garbage value as boolean"() {
        when:
        FieldValueParser.parseBoolean(source, srcIndex)

        then:
        thrown(IllegalArgumentException)

        where:
        source            | srcIndex
        comp("testYtest") | 0
        comp("a")         | 0
        comp("b")         | 0
        comp("\u0001")    | 0
        comp("0")         | 0
        comp("1")         | 0
    }

    def "should parse char"() {
        expect:
        FieldValueParser.parseChar(source, srcIndex) == expectedValue

        where:
        source       | srcIndex || expectedValue
        comp("test") | 0        || 't' as char
        comp("test") | 1        || 'e' as char
        comp("test") | 2        || 's' as char
        comp("test") | 3        || 't' as char
        comp("123")  | 0        || '1' as char
        comp("123")  | 1        || '2' as char
        comp("123")  | 2        || '3' as char
    }

    def "should parse text from buffer"() {
        setup:
        def byteBuff = Unpooled.buffer(CAPACITY, CAPACITY)
        byteBuff.writeCharSequence("test", StandardCharsets.US_ASCII)
        byteBuff.writeCharSequence("test2", StandardCharsets.US_ASCII)
        byteBuff.readerIndex(4)
        def composer = new ByteBufComposer(1)
        composer.addByteBuf(byteBuff)
        def tempBuf = Unpooled.buffer(5, 5)
        def chars = new char[5]

        when:
        FieldValueParser.readChars(composer, 0, 5, tempBuf, chars)

        then:
        tempBuf.array() == [116, 101, 115, 116, 116] as byte[]
        chars == ['t', 'e', 's', 't', 't'] as char[]
        byteBuff.readerIndex() == 4 //yup reader index of underlying ByteBuf is not modified
        byteBuff.writerIndex() == 9
    }

    def "should parse double"() {
        setup:
        def byteBuff = Unpooled.buffer(CAPACITY, CAPACITY)
        byteBuff.writeCharSequence(rawValue, StandardCharsets.US_ASCII)
        byteBuff.readerIndex(0)
        def composer = new ByteBufComposer(1)
        composer.addByteBuf(byteBuff)
        def tempBuf = Unpooled.buffer(rawValue.length(), rawValue.length())
        def chars = new char[rawValue.length()]
        long value
        short scale

        when:
        FieldValueParser.setDoubleValuesFromByteBufComposer(composer, 0, rawValue.length(), tempBuf, chars, (longValue, shortValue) -> {
            value = longValue
            scale = shortValue
        })

        then:
        value == expectedValue
        scale == expectedScale

        where:
        rawValue        | expectedValue | expectedScale
        "123"        | 123           | 0 as short
        "-123"       | -123          | 0 as short
        "123."       | 123           | 0 as short
        "123.45"     | 12345         | 2 as short
        "0000123.45" | 12345         | 2 as short
    }

    private static ByteBufComposer comp(String value) {
        ByteBufComposer composer = new ByteBufComposer(1)
        composer.addByteBuf(Unpooled.wrappedBuffer(value.getBytes(StandardCharsets.US_ASCII)))
        return composer
    }
}
