package io.github.zlooo.fixyou.parser.model

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class CharFieldTest extends Specification {

    private CharField field

    void setup() {
        field = new CharField(1)
        field.fieldData.writeCharSequence("A", StandardCharsets.US_ASCII)
    }

    def "should get value"() {
        when:
        def value = field.getValue()

        then:
        value == 'A' as char
        field.@value == 'A' as char
    }

    def "should cache value once parsed"() {
        setup:
        field.getValue()
        field.fieldData.clear().writeCharSequence("B", StandardCharsets.US_ASCII);

        expect:
        field.getValue() == 'A' as char
    }

    def "should reset state"() {
        when:
        field.resetInnerState()

        then:
        field.@value == Character.MIN_VALUE
    }

    def "should set value"() {
        when:
        field.setValue('B' as char)

        then:
        field.@value == 'B' as char
        field.getValue() == 'B' as char
        ByteBuf expectedBuffer = Unpooled.buffer(10)
        expectedBuffer.writeCharSequence("B", StandardCharsets.US_ASCII)
        field.fieldData.compareTo(expectedBuffer) == 0
    }
}
