package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.parser.TestSpec
import io.github.zlooo.fixyou.parser.TestUtils
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor

class CharSequenceFieldTest extends Specification {

    private Field field
    private ByteBuf underlyingBuf = Unpooled.buffer(30, 30)
    private Executor executor = Mock()

    void setup() {
        field = new Field(1, new FieldCodec());
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
        Field field2 = new Field(2, new FieldCodec())
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
        def result = field.appendByteBufWithValue(buf, TestSpec.INSTANCE, executor)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "textToWrite"
        result == TestUtils.sumBytes("textToWrite".getBytes(StandardCharsets.US_ASCII))
    }

    def "should copy unparsed value from other field"() {
        setup:
        Field newField = new Field(5, new FieldCodec())

        when:
        newField.copyDataFrom(field)

        then:
        newField.@fieldValue.charArrayValue.every { it == 0 }
        newField.@fieldValue.length == 0
        newField.@fieldValue.rawValue.writerIndex() == 0
        newField.@fieldValue.rawValue.readerIndex() == 0
        newField.charSequenceValue.toString() == "test"
        newField.startIndex == 1
        newField.endIndex == 5
        newField.indicesSet
        newField.valueSet
        newField.fieldData.is(field.fieldData)
    }

    def "should copy previously set value"() {
        setup:
        Field existingField = new Field(10, new FieldCodec())
        def value = "some veeeeery long value that will require internal array resize"
        existingField.charSequenceValue = value.chars
        Field newField = new Field(11, new FieldCodec())

        when:
        newField.copyDataFrom(existingField)

        then:
        newField.@fieldValue.charSequenceValue.toString() == value
        newField.@fieldValue.charArrayValue == resize(value.chars, newField.@fieldValue.charArrayValue.length)
        newField.@fieldValue.length == value.length()
        newField.@fieldValue.rawValue.toString(0, value.length(), StandardCharsets.US_ASCII) == value
        newField.charSequenceValue.toString() == value
        newField.startIndex == 0
        newField.endIndex == 0
        !newField.indicesSet
        newField.valueSet
        newField.fieldData == null
    }

    private char[] resize(char[] source, length) {
        char[] array = new char[length]
        System.arraycopy(source, 0, array, 0, source.length)
        return array
    }
}
