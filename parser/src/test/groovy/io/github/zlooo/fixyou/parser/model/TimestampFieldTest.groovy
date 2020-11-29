package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.parser.TestSpec
import io.github.zlooo.fixyou.parser.TestUtils
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.Executor

class TimestampFieldTest extends Specification {

    private Field field
    private long millis = Instant.parse("2020-06-08T22:45:16.666Z").toEpochMilli()
    private ByteBuf underlyingBuf = Unpooled.buffer(30, 30)
    private Executor executor = Mock()

    void setup() {
        field = new Field(1, new FieldCodec())
        def byteBufComposer = new ByteBufComposer(1)
        field.setFieldData(byteBufComposer)
        underlyingBuf.clear().writeCharSequence("20200608-22:45:16.666", StandardCharsets.US_ASCII)
        byteBufComposer.addByteBuf(underlyingBuf)
        field.setIndexes(0, 21)
    }

    def "should get timestamp value"() {
        expect:
        field.timestampValue == millis
        field.valueSet
    }

    def "should get default value when value is not set"() {
        setup:
        field.reset()

        expect:
        field.timestampValue == FieldValue.LONG_DEFAULT_VALUE
    }

    def "should cache value once parsed"() {
        setup:
        field.timestampValue
        underlyingBuf.clear().writeCharSequence("!", StandardCharsets.US_ASCII)
        field.setIndexes(0, 1)

        expect:
        field.timestampValue == millis
        field.valueSet
    }

    def "should reset state"() {
        when:
        field.reset()

        then:
        field.@fieldValue.longValue == FieldValue.LONG_DEFAULT_VALUE
        field.@fieldValue.sumOfBytes == 0
        field.@fieldValue.rawValue.readerIndex() == 0
        field.@fieldValue.rawValue.writerIndex() == 0
    }

    def "should set value"() {
        when:
        field.timestampValue = 1600241144694

        then:
        field.@fieldValue.longValue == 1600241144694
        field.@fieldValue.rawValue.toString(0, field.@fieldValue.length, StandardCharsets.US_ASCII) == "20200916-07:25:44.694"
        field.fieldValue.sumOfBytes == TestUtils.sumBytes("20200916-07:25:44.694".getBytes(StandardCharsets.US_ASCII))
        field.valueSet
    }

    def "should append provided byte buf with value"() {
        setup:
        field.timestampValue = Instant.parse("2026-06-06T22:45:16.666Z").toEpochMilli()
        def buf = Unpooled.buffer(30, 30)

        when:
        def result = field.appendByteBufWithValue(buf, TestSpec.INSTANCE, executor)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "20260606-22:45:16.666"
        result == TestUtils.sumBytes("20260606-22:45:16.666".getBytes(StandardCharsets.US_ASCII))
    }

    def "should copy unparsed value from other field"() {
        setup:
        Field newField = new Field(5, new FieldCodec())

        when:
        newField.copyDataFrom(field)

        then:
        newField.@fieldValue.longValue == FieldValue.LONG_DEFAULT_VALUE
        newField.timestampValue == millis
        newField.startIndex == 0
        newField.endIndex == 21
        newField.indicesSet
        newField.valueSet
        newField.fieldData.is(field.fieldData)
    }

    def "should copy previously set value"() {
        setup:
        Field existingField = new Field(10, new FieldCodec())
        existingField.timestampValue = millis
        Field newField = new Field(11, new FieldCodec())

        when:
        newField.copyDataFrom(existingField)

        then:
        newField.@fieldValue.longValue == millis
        newField.@fieldValue.length == 21
        newField.@fieldValue.rawValue.toString(0, 21, StandardCharsets.US_ASCII) == "20200608-22:45:16.666"
        newField.timestampValue == millis
        newField.startIndex == 0
        newField.endIndex == 0
        !newField.indicesSet
        newField.valueSet
        newField.fieldData == null
    }
}
