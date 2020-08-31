package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class LongFieldTest extends Specification {

    private LongField field
        private ByteBuf underlyingBuf = Unpooled.buffer(10, 10)

    void setup() {
        field = new LongField(1)
        def byteBufComposer = new ByteBufComposer(1)
        field.setFieldData(byteBufComposer)
        underlyingBuf.clear().writeCharSequence("123456", StandardCharsets.US_ASCII)
        underlyingBuf.writeByte(0x01)
        byteBufComposer.addByteBuf(underlyingBuf)
        field.setIndexes(0, 7)
    }

    def "should get value"() {
        expect:
        field.getValue() == 123456
        field.valueSet
    }

    def "should get default value when value is not set"() {
        setup:
        field.reset()

        expect:
        field.value == LongField.DEFAULT_VALUE
    }

    def "should cache value once parsed"() {
        setup:
        field.getValue()
        underlyingBuf.clear().writeCharSequence("!", StandardCharsets.US_ASCII)
        field.setIndexes(0, 1)

        expect:
        field.getValue() == 123456
        field.valueSet
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
        field.valueSet
    }

    def "should append provided byte buf with value"() {
        setup:
        field.value = 14666
        def buf = Unpooled.buffer(10, 10)

        when:
        field.appendByteBufWithValue(buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "14666"
    }
}
