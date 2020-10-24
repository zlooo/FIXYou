package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.parser.TestSpec
import io.github.zlooo.fixyou.parser.TestUtils
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class CharSequenceFieldTest extends Specification {

    private Field field
    private ByteBuf underlyingBuf = Unpooled.buffer(30, 30)

    void setup() {
        field = new Field(1, TestSpec.INSTANCE, new FieldCodec());
        def byteBufComposer = new ByteBufComposer(1)
        field.setFieldData(byteBufComposer)
        underlyingBuf.writerIndex(1)
        underlyingBuf.writeCharSequence("test", StandardCharsets.US_ASCII)
        byteBufComposer.addByteBuf(underlyingBuf)
        field.setIndexes(1, 5)
    }

    def "should get char sequence value"() {
        expect:
        field.charSequenceValue.toString() == "test"
    }

    def "should get default value when value is not set"() {
        setup:
        field.reset()

        expect:
        field.charSequenceValue != null
        field.charSequenceValue.length() == 0
    }

    def "should cache value once parsed"() {
        setup:
        field.charSequenceValue
        underlyingBuf.clear().writeCharSequence("valueThatShouldBeIgnored", StandardCharsets.US_ASCII)

        expect:
        field.charSequenceValue.toString() == "test"
    }

    def "should reset state"() {
        when:
        field.reset()

        then:
        field.@fieldValue.length == 0
        field.@fieldValue.charSequenceValue.length() == 0
        field.@fieldValue.sumOfBytes == 0
    }

    def "should set value from char array"() {
        when:
        field.setCharSequenceValue("someVeryLongValue".toCharArray())

        then:
        field.@fieldValue.charSequenceValue.toString() == "someVeryLongValue"
        field.@fieldValue.length == "someVeryLongValue".length()
        field.valueSet
        field.@fieldValue.rawValue.toString(0, field.@fieldValue.rawValue.writerIndex(), StandardCharsets.US_ASCII) == "someVeryLongValue"
        field.@fieldValue.sumOfBytes == TestUtils.sumBytes("someVeryLongValue".getBytes(StandardCharsets.US_ASCII))
    }

    def "should set value from char sequence"() {
        setup:
        Field field2 = new Field(2, TestSpec.INSTANCE, new FieldCodec())
        field2.setCharSequenceValue("someVeryLongValue".toCharArray(), "someVeryLongValue".length())

        when:
        field.setCharSequenceValue(field2)

        then:
        field.@fieldValue.charSequenceValue.toString() == "someVeryLongValue"
        field.@fieldValue.charSequenceValue.length() == "someVeryLongValue".length()
        field.@fieldValue.length == "someVeryLongValue".length()
        field.valueSet
        field.@fieldValue.rawValue.toString(0, field.@fieldValue.rawValue.writerIndex(), StandardCharsets.US_ASCII) == "someVeryLongValue"
        field.@fieldValue.sumOfBytes == TestUtils.sumBytes("someVeryLongValue".getBytes(StandardCharsets.US_ASCII))
    }

    def "should set value from char array with limited length"() {
        when:
        field.setCharSequenceValue("someVeryLongValue".toCharArray(), 4)

        then:
        field.@fieldValue.charSequenceValue.toString() == "some"
        field.@fieldValue.charSequenceValue.length() == 4
        field.valueSet
        field.@fieldValue.rawValue.toString(0, field.@fieldValue.rawValue.writerIndex(), StandardCharsets.US_ASCII) == "some"
        field.@fieldValue.sumOfBytes == TestUtils.sumBytes("some".getBytes(StandardCharsets.US_ASCII))
    }

    def "should append provided byte buf with value"() {
        setup:
        field.charSequenceValue = "textToWrite".toCharArray()
        def buf = Unpooled.buffer(20, 20)

        when:
        def result = field.appendByteBufWithValue(buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "textToWrite"
        result == TestUtils.sumBytes("textToWrite".getBytes(StandardCharsets.US_ASCII))
    }
}
