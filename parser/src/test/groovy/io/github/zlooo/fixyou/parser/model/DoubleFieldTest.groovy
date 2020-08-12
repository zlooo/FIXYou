package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class DoubleFieldTest extends Specification {
    private static final String VALUE_THATS_SUPPOSED_TO_BE_IGNORED = "!"
    private DoubleField field
    private ByteBuf underlyingBuf = Unpooled.buffer(20, 20)

    void setup() {
        field = new DoubleField(10)
        def byteBufComposer = new ByteBufComposer(1)
        field.setFieldData(byteBufComposer)
        underlyingBuf.writeCharSequence("-123.666", StandardCharsets.US_ASCII)
        byteBufComposer.addByteBuf(underlyingBuf)
        field.setIndexes(0, 8)
    }

    def "should return proper values"() {
        expect:
        field.value == -123666L
        field.scale == 3 as short
        field.valueSet
    }

    def "should parse value"() {
        setup:
        field.reset()
        field.fieldData.releaseDataUpTo(Integer.MAX_VALUE)
        underlyingBuf.clear().writeCharSequence(valueToParse, StandardCharsets.US_ASCII)
        field.fieldData.addByteBuf(underlyingBuf)

        when:
        field.setIndexes(0, valueToParse.length())

        then:
        field.value == expectedValue
        field.scale == expectedScale
        field.valueSet

        where:
        valueToParse        | expectedValue     | expectedScale
        "123456"            | 123456L           | 0 as short
        "-123456"           | -123456L          | 0 as short
        "123456."           | 123456L           | 0 as short
        "123456.000"        | 123456000L        | 3 as short
        "-123456."          | -123456L          | 0 as short
        "123456.123456789"  | 123456123456789L  | 9 as short
        "-123456.123456789" | -123456123456789L | 9 as short
        "000123456.123456"  | 123456123456L     | 6 as short
        "-000123456.123456" | -123456123456L    | 6 as short
    }

    def "should get default value when value is not set"() {
        setup:
        field.reset()

        expect:
        field.value == DoubleField.DEFAULT_VALUE
        field.scale == 0
    }

    def "should cache value after first get"() {
        setup:
        field.value

        when:
        underlyingBuf.clear().writeCharSequence(VALUE_THATS_SUPPOSED_TO_BE_IGNORED, StandardCharsets.US_ASCII)
        field.setIndexes(0, 1)

        then:
        field.value == -123666L
        field.scale == 3 as short
        field.valueSet
    }

    def "should cache scale after first get"() {
        setup:
        field.scale

        when:
        underlyingBuf.clear().writeCharSequence(VALUE_THATS_SUPPOSED_TO_BE_IGNORED, StandardCharsets.US_ASCII)
        field.setIndexes(0, 1)

        then:
        field.value == -123666L
        field.scale == 3 as short
        field.valueSet
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
        field.valueSet
    }

    def "should append provided byte buf with value"() {
        setup:
        field.setValue(1472, 3 as short)
        def buf = Unpooled.buffer(10, 10)

        when:
        field.appendByteBufWithValue(buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "1.472"
    }
}
