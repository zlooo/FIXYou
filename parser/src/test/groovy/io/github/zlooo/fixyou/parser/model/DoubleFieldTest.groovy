package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.parser.TestSpec
import io.github.zlooo.fixyou.parser.TestUtils
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor

class DoubleFieldTest extends Specification {
    private static final String VALUE_THATS_SUPPOSED_TO_BE_IGNORED = "!"
    private Field field
    private ByteBuf underlyingBuf = Unpooled.buffer(20, 20)
    private Executor executor = Mock()

    void setup() {
        field = new Field(10, new FieldCodec())
        def byteBufComposer = new ByteBufComposer(1)
        field.setFieldData(byteBufComposer)
        underlyingBuf.writeCharSequence("-123.666", StandardCharsets.US_ASCII)
        byteBufComposer.addByteBuf(underlyingBuf)
        field.setIndexes(0, 8)
    }

    def "should return proper double values"() {
        expect:
        field.doubleUnscaledValue == -123666L
        field.scale == 3 as short
        field.valueSet
    }

    def "should parse value"() {
        setup:
        field.reset()
        field.fieldData.releaseData(0, Integer.MAX_VALUE)
        underlyingBuf.clear().writeCharSequence(valueToParse, StandardCharsets.US_ASCII)
        field.fieldData.addByteBuf(underlyingBuf)

        when:
        field.setIndexes(0, valueToParse.length())

        then:
        field.doubleUnscaledValue == expectedValue
        field.scale == expectedScale
        field.valueSet
        field.@fieldValue.longValue == expectedValue
        field.@fieldValue.scale == expectedScale

        where:
        valueToParse        | expectedValue     | expectedScale
        "123456"            | 123456L           | 0 as short
        "-123456"           | -123456L          | 0 as short
        "123456."           | 123456L           | 0 as short
        "123456.000"        | 123456000L        | 3 as short
        "-123456."          | -123456L          | 0 as short
        "123456.123456789"  | 123456123456789L  | 9 as short
        "-123456.123456789" | -123456123456789L | 9 as short
        "000123456.123456"  | 123456123456L     | 6 as short
        "-000123456.123456" | -123456123456L    | 6 as short
    }

    def "should get default value when value is not set"() {
        setup:
        field.reset()

        expect:
        field.doubleUnscaledValue == FieldValue.LONG_DEFAULT_VALUE
        field.scale == 0 as short
    }

    def "should cache value after first get"() {
        setup:
        field.doubleUnscaledValue

        when:
        underlyingBuf.clear().writeCharSequence(VALUE_THATS_SUPPOSED_TO_BE_IGNORED, StandardCharsets.US_ASCII)
        field.setIndexes(0, 1)

        then:
        field.doubleUnscaledValue == -123666L
        field.scale == 3 as short
        field.valueSet
        field.@fieldValue.longValue == -123666L
        field.@fieldValue.scale == 3 as short
    }

    def "should cache scale after first get"() {
        setup:
        field.scale

        when:
        underlyingBuf.clear().writeCharSequence(VALUE_THATS_SUPPOSED_TO_BE_IGNORED, StandardCharsets.US_ASCII)
        field.setIndexes(0, 1)

        then:
        field.doubleUnscaledValue == -123666L
        field.scale == 3 as short
        field.valueSet
        field.@fieldValue.longValue == -123666L
        field.@fieldValue.scale == 3 as short
    }

    def "should reset state"() {
        when:
        field.reset()

        then:
        field.@fieldValue.longValue == FieldValue.LONG_DEFAULT_VALUE
        field.@fieldValue.scale == 0 as short
        field.@fieldValue.sumOfBytes == 0
        field.@fieldValue.length == 0
    }

    def "should set value"() {
        when:
        field.setDoubleValue(666777L, 3 as short)

        then:
        field.@fieldValue.longValue == 666777L
        field.@fieldValue.scale == 3 as short
        field.@fieldValue.length == 7
        field.@fieldValue.rawValue.toString(0, field.@fieldValue.rawValue.writerIndex(), StandardCharsets.US_ASCII) == "666.777"
        field.@fieldValue.sumOfBytes == TestUtils.sumBytes("666.777".getBytes(StandardCharsets.US_ASCII))
        field.valueSet
    }

    def "should append provided byte buf with value"() {
        setup:
        field.setDoubleValue(1472, 3 as short)
        def buf = Unpooled.buffer(10, 10)

        when:
        def result = field.appendByteBufWithValue(buf, TestSpec.INSTANCE, executor)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "1.472"
        result == TestUtils.sumBytes("1.472".getBytes(StandardCharsets.US_ASCII))
    }

    def "should copy unparsed value from other field"() {
        setup:
        Field newField = new Field(5, new FieldCodec())

        when:
        newField.copyDataFrom(field)

        then:
        newField.@fieldValue.longValue == FieldValue.LONG_DEFAULT_VALUE
        newField.@fieldValue.scale == 0
        newField.@fieldValue.length == 0
        newField.doubleUnscaledValue == -123666
        newField.scale == 3
        newField.startIndex == 0
        newField.endIndex == 8
        newField.indicesSet
        newField.valueSet
        newField.fieldData.is(field.fieldData)
    }

    def "should copy previously set value"() {
        setup:
        Field existingField = new Field(10, new FieldCodec())
        existingField.setDoubleValue(769667, 3 as short)
        Field newField = new Field(11, new FieldCodec())

        when:
        newField.copyDataFrom(existingField)

        then:
        newField.@fieldValue.longValue == 769667
        newField.@fieldValue.scale == 3
        newField.@fieldValue.length == 7
        newField.@fieldValue.rawValue.toString(0, 7, StandardCharsets.US_ASCII) == "769.667"
        newField.doubleUnscaledValue == 769667
        newField.scale == 3
        newField.startIndex == 0
        newField.endIndex == 0
        !newField.indicesSet
        newField.valueSet
        newField.fieldData == null
    }
}
