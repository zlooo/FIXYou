package io.github.zlooo.fixyou.parser.model

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class DoubleFieldTest extends Specification {
    private DoubleField field

    void setup() {
        field = new DoubleField(10)
        field.fieldData.writeCharSequence("-123.666", StandardCharsets.US_ASCII)
    }

    def "should return proper values"() {
        expect:
        field.value == -123666L
        field.scale == 3 as short
    }

    def "should parse value"() {
        setup:
        field.reset()

        when:
        field.fieldData.writeCharSequence(valueToParse, StandardCharsets.US_ASCII)

        then:
        field.value == expectedValue
        field.scale == expectedScale

        where:
        valueToParse           | expectedValue     | expectedScale
        "123456"               | 123456L           | 0 as short
        "-123456"              | -123456L          | 0 as short
        "123456."              | 123456L           | 0 as short
        "123456.000"           | 123456000L        | 3 as short
        "-123456."             | -123456L          | 0 as short
        "123456.123456789"     | 123456123456789L  | 9 as short
        "-123456.123456789"    | -123456123456789L | 9 as short
        "000123456.123456789"  | 123456123456789L  | 9 as short
        "-000123456.123456789" | -123456123456789L | 9 as short
    }

    def "should cache value after first get"() {
        setup:
        field.value

        when:
        field.fieldData.clear().writeCharSequence("valueThatShouldBeIgnored", StandardCharsets.US_ASCII)

        then:
        field.value == -123666L
        field.scale == 3 as short
    }

    def "should cache scale after first get"() {
        setup:
        field.scale

        when:
        field.fieldData.clear().writeCharSequence("valueThatShouldBeIgnored", StandardCharsets.US_ASCII)

        then:
        field.value == -123666L
        field.scale == 3 as short
    }

    def "should reset state"() {
        when:
        field.resetInnerState()

        then:
        field.@value == DoubleField.DEFAULT_VALUE
        field.@scale == 0 as short
    }

    def "should set value"() {
        when:
        field.setValue(666777L, 3 as short)

        then:
        field.@value == 666777L
        field.@scale == 3 as short
        ByteBuf expectedBuffer = Unpooled.buffer(10)
        expectedBuffer.writeCharSequence("666.777", StandardCharsets.US_ASCII)
        field.fieldData.compareTo(expectedBuffer) == 0
    }
}
