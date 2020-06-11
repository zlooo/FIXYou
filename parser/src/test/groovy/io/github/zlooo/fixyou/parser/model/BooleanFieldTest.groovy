package io.github.zlooo.fixyou.parser.model


import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class BooleanFieldTest extends Specification {

    private BooleanField field

    void setup() {
        field = new BooleanField(1)
        field.fieldData = Unpooled.buffer(10, 10)
        field.fieldData.writerIndex(5)
        field.fieldData.writeCharSequence("Y", StandardCharsets.US_ASCII)
        field.setIndexes(5, 6)
    }

    def "should get value"() {
        setup:
        field = new BooleanField(1)
        field.fieldData = Unpooled.buffer(10, 10)
        field.fieldData.writerIndex(5)
        field.fieldData.writeCharSequence(rawValue, StandardCharsets.US_ASCII)
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

    def "should get default value when value is not set"(){
        setup:
        field.reset()

        expect:
        field.value == false
    }

    def "should throw exception when trying to parse unexpected value"() {
        setup:
        field = new BooleanField(1)
        field.fieldData = Unpooled.buffer(10, 10)
        field.fieldData.writeCharSequence("Z", StandardCharsets.US_ASCII)
        field.setIndexes(0, 1)

        when:
        def value = field.getValue()

        then:
        thrown(IllegalArgumentException)
    }

    def "should cache value once parsed"() {
        setup:
        field.getValue()
        field.fieldData.clear().writeCharSequence("!", StandardCharsets.US_ASCII);

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
        field.appendByteBufWithValue(buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == expectedValue

        where:
        valueToSet | expectedValue
        true       | "Y"
        false      | "N"
    }
}
