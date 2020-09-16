package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.AsciiString
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class CharFieldTest extends Specification {

    private CharField field
    private ByteBuf underlyingBuf =Unpooled.buffer(10, 10)

    void setup() {
        field = new CharField(1)
        def byteBufComposer = new ByteBufComposer(1)
        field.setFieldData(byteBufComposer)
        underlyingBuf.writerIndex(5)
        underlyingBuf.writeCharSequence("A", StandardCharsets.US_ASCII)
        byteBufComposer.addByteBuf(underlyingBuf)
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
        underlyingBuf.clear().writeCharSequence("B", StandardCharsets.US_ASCII);
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
        field.valueRaw == AsciiString.c2b('B' as char)
        field.valueSet
        underlyingBuf.readerIndex(5).toString(StandardCharsets.US_ASCII) == "A"
    }

    def "should append provided byte buf with value"() {
        setup:
        field.value = 'Z' as char
        def buf = Unpooled.buffer(1, 1)

        when:
        def result = field.appendByteBufWithValue(buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "Z"
        result == AsciiString.c2b('Z' as char)
    }
}
