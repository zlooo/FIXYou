package io.github.zlooo.fixyou.parser.model

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class LongFieldTest extends Specification {

    private LongField field

    void setup() {
        field = new LongField(1)
        field.fieldData.clear().writeCharSequence("123456", StandardCharsets.US_ASCII)
    }

    def "should get value"() {
        expect:
        field.getValue() == 123456
    }

    def "should cache value once parsed"() {
        setup:
        field.getValue()
        field.fieldData.clear().writeCharSequence("valueThatShouldBeIgnored", StandardCharsets.US_ASCII)

        expect:
        field.getValue() == 123456
    }

    def "should reset state"() {
        when:
        field.resetInnerState()

        then:
        field.@value == LongField.DEFAULT_VALUE
    }

    def "should set value"() {
        when:
        field.setValue(666)

        then:
        field.@value == 666
        ByteBuf expectedBuffer = Unpooled.buffer(10)
        expectedBuffer.writeCharSequence("666", StandardCharsets.US_ASCII)
        field.fieldData.compareTo(expectedBuffer) == 0
    }
}
