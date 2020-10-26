package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.parser.TestSpec
import io.github.zlooo.fixyou.parser.TestUtils
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.AsciiString
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class LongFieldTest extends Specification {

    private Field field
    private ByteBuf underlyingBuf = Unpooled.buffer(10, 10)

    void setup() {
        field = new Field(1, new FieldCodec())
        def byteBufComposer = new ByteBufComposer(1)
        field.setFieldData(byteBufComposer)
        underlyingBuf.clear().writeCharSequence("123456", StandardCharsets.US_ASCII)
        underlyingBuf.writeByte(0x01)
        byteBufComposer.addByteBuf(underlyingBuf)
        field.setIndexes(0, 7)
    }

    def "should get long value"() {
        expect:
        field.longValue == 123456
        field.valueSet
    }

    def "should get default value when value is not set"() {
        setup:
        field.reset()

        expect:
        field.longValue == FieldValue.LONG_DEFAULT_VALUE
    }

    def "should cache value once parsed"() {
        setup:
        field.longValue
        underlyingBuf.clear().writeCharSequence("!", StandardCharsets.US_ASCII)
        field.setIndexes(0, 1)

        expect:
        field.longValue == 123456
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
        field.longValue=666

        then:
        field.@fieldValue.longValue == 666
        field.@fieldValue.rawValue.toString(0, 3, StandardCharsets.US_ASCII) == "666"
        field.@fieldValue.rawValue.readerIndex() == 0
        field.@fieldValue.rawValue.writerIndex() == 3
        field.@fieldValue.sumOfBytes == 3 * AsciiString.c2b("6" as char)
        field.valueSet
    }

    def "should append provided byte buf with value"() {
        setup:
        field.longValue = 14666
        def buf = Unpooled.buffer(10, 10)

        when:
        def result = field.appendByteBufWithValue(buf, TestSpec.INSTANCE)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "14666"
        result == TestUtils.sumBytes("14666".getBytes(StandardCharsets.US_ASCII))
    }
}
