package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.AsciiString
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class BooleanFieldTest extends Specification {

    private BooleanField field
    private ByteBuf underlyingBuf = Unpooled.buffer(10, 10)

    void setup() {
        field = new BooleanField(1)
        field.fieldData = new ByteBufComposer(1)
        underlyingBuf.writerIndex(5)
        underlyingBuf.writeCharSequence("Y", StandardCharsets.US_ASCII)
        field.fieldData.addByteBuf(underlyingBuf)
        field.setIndexes(5, 6)
    }

    def "should get value"() {
        setup:
        field = new BooleanField(1)
        field.fieldData = new ByteBufComposer(1)
        underlyingBuf.writerIndex(5)
        underlyingBuf.writeCharSequence(rawValue, StandardCharsets.US_ASCII)
        field.fieldData.addByteBuf(underlyingBuf)
        field.setIndexes(5, 6)

        when:
        def value = field.getValue()

        then:
        value == expectedResult
        field.@parsed

        where:
        rawValue | expectedResult
        "Y"      | true
        "N"      | false
    }

    def "should get default value when value is not set"() {
        setup:
        field.reset()

        expect:
        field.value == false
    }

    def "should throw exception when trying to parse unexpected value"() {
        setup:
        field = new BooleanField(1)
        underlyingBuf.writeCharSequence("Z", StandardCharsets.US_ASCII)
        field.fieldData = new ByteBufComposer(1)
        field.fieldData.addByteBuf(underlyingBuf)
        field.setIndexes(0, 1)

        when:
        field.getValue()

        then:
        thrown(IllegalArgumentException)
    }

    def "should cache value once parsed"() {
        setup:
        field.getValue()
        field.fieldData.releaseData(0, 666)

        expect:
        field.getValue()
    }

    def "should reset state"() {
        when:
        field.resetInnerState()

        then:
        !field.@parsed
    }

    def "should set value"() {
        when:
        field.setValue(false)

        then:
        field.@parsed
        field.valueSet
        !field.getValue()
    }

    def "should append provided byte buf with value"() {
        setup:
        field.value = valueToSet
        def buf = Unpooled.buffer(1, 1)

        when:
        def result = field.appendByteBufWithValue(buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == expectedValue
        result == expectedResult

        where:
        valueToSet | expectedValue | expectedResult
        true       | "Y"           | AsciiString.c2b('Y' as char)
        false      | "N"           | AsciiString.c2b('N' as char)
    }
}
