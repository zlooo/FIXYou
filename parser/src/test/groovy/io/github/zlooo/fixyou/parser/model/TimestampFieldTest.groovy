package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Instant

class TimestampFieldTest extends Specification {

    private TimestampField field
    private long millis = Instant.parse("2020-06-08T22:45:16.666Z").toEpochMilli()
    private ByteBuf underlyingBuf = Unpooled.buffer(30, 30)

    void setup() {
        field = new TimestampField(1)
        def byteBufComposer = new ByteBufComposer(1)
        field.setFieldData(byteBufComposer)
        underlyingBuf.clear().writeCharSequence("20200608-22:45:16.666", StandardCharsets.US_ASCII)
        byteBufComposer.addByteBuf(underlyingBuf)
        field.setIndexes(0, 21)
    }

    def "should get value"() {
        expect:
        field.getValue() == millis
        field.valueSet
    }

    def "should get default value when value is not set"() {
        setup:
        field.reset()

        expect:
        field.value == TimestampField.DEFAULT_VALUE
    }

    def "should cache value once parsed"() {
        setup:
        field.getValue()
        underlyingBuf.clear().writeCharSequence("!", StandardCharsets.US_ASCII)
        field.setIndexes(0, 1)

        expect:
        field.getValue() == millis
        field.valueSet
    }

    def "should reset state"() {
        when:
        field.resetInnerState()

        then:
        field.@value == TimestampField.DEFAULT_VALUE
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
        field.value = Instant.parse("2026-06-06T22:45:16.666Z").toEpochMilli()
        def buf = Unpooled.buffer(30, 30)

        when:
        field.appendByteBufWithValue(buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "20260606-22:45:16.666"
    }
}
