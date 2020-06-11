package io.github.zlooo.fixyou.parser.model

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class CharFieldTest extends Specification {

    private CharField field

    void setup() {
        field = new CharField(1)
        field.fieldData = Unpooled.buffer(10, 10)
        field.fieldData.writerIndex(5)
        field.fieldData.writeCharSequence("A", StandardCharsets.US_ASCII)
        field.setIndexes(5, 6)
    }

    def "should get value"() {
        when:
        def value = field.getValue()

        then:
        value == 'A' as char
        field.@value == 'A' as char
    }

    def "should get default value when value is not set"(){
        setup:
        field.reset()

        expect:
        field.value == CharField.DEFAULT_VALUE
    }

    def "should cache value once parsed"() {
        setup:
        field.getValue()
        field.fieldData.clear().writeCharSequence("B", StandardCharsets.US_ASCII);
        field.setIndexes(0,1)

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
        field.valueSet
        field.fieldData.readerIndex(5).toString(StandardCharsets.US_ASCII) == "A"
    }

    def "should append provided byte buf with value"() {
        setup:
        field.value = 'Z' as char
        def buf = Unpooled.buffer(1, 1)

        when:
        field.appendByteBufWithValue(buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "Z"
    }
}
