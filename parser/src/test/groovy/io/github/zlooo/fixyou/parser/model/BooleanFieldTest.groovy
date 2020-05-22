package io.github.zlooo.fixyou.parser.model

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class BooleanFieldTest extends Specification {

    private BooleanField field

    void setup() {
        field = new BooleanField(1)
        field.fieldData.writeCharSequence("Y", StandardCharsets.US_ASCII)
    }

    def "should get value"() {
        setup:
        field = new BooleanField(1)
        field.fieldData.writeCharSequence(rawValue, StandardCharsets.US_ASCII)

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

    def "should throw exception when trying to parse unexpected value"() {
        setup:
        field = new BooleanField(1)
        field.fieldData.writeCharSequence("Z", StandardCharsets.US_ASCII)

        when:
        def value = field.getValue()

        then:
        thrown(IllegalArgumentException)
    }

    def "should cache value once parsed"() {
        setup:
        field.getValue()
        field.fieldData.clear().writeCharSequence("valueThatShouldBeIgnored", StandardCharsets.US_ASCII);

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
        !field.getValue()
        ByteBuf expectedBuffer = Unpooled.buffer(10)
        expectedBuffer.writeCharSequence("N", StandardCharsets.US_ASCII)
        field.fieldData.compareTo(expectedBuffer) == 0
    }
}
