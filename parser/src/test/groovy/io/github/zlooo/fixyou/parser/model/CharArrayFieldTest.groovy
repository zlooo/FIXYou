package io.github.zlooo.fixyou.parser.model

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class CharArrayFieldTest extends Specification {

    private CharArrayField field

    void setup() {
        field = new CharArrayField( 1);
        field.fieldData.writeCharSequence("test", StandardCharsets.US_ASCII)
    }

    def "should get value"() {
        expect:
        field.getValue() == "test".toCharArray()
    }

    def "should cache value once parsed"() {
        setup:
        field.getValue()
        field.fieldData.clear().writeCharSequence("valueThatShouldBeIgnored", StandardCharsets.US_ASCII)

        expect:
        field.getValue() == "test".toCharArray()
    }

    def "should reset state"() {
        when:
        field.resetInnerState()

        then:
        field.@value == null
    }

    def "should set value"() {
        when:
        field.setValue("someValue".toCharArray())

        then:
        field.@value == "someValue".toCharArray()
        ByteBuf expectedBuffer = Unpooled.buffer(10)
        expectedBuffer.writeCharSequence("someValue", StandardCharsets.US_ASCII)
        field.fieldData.compareTo(expectedBuffer) == 0
    }
}
