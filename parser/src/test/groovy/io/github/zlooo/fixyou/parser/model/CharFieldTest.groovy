package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.parser.TestSpec
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.AsciiString
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class CharFieldTest extends Specification {

    private Field field
    private ByteBuf underlyingBuf = Unpooled.buffer(10, 10)

    void setup() {
        field = new Field(1, new FieldCodec())
        def byteBufComposer = new ByteBufComposer(1)
        field.setFieldData(byteBufComposer)
        underlyingBuf.writerIndex(5)
        underlyingBuf.writeCharSequence("A", StandardCharsets.US_ASCII)
        byteBufComposer.addByteBuf(underlyingBuf)
        field.setIndexes(5, 6)
    }

    def "should get char value"() {
        when:
        def value = field.charValue

        then:
        value == 'A' as char
        field.@fieldValue.charValue == 'A' as char
    }

    def "should get default value when value is not set"() {
        setup:
        field.reset()

        expect:
        field.charValue == FieldValue.CHAR_DEFAULT_VALUE
    }

    def "should cache value once parsed"() {
        setup:
        field.charValue
        underlyingBuf.clear().writeCharSequence("B", StandardCharsets.US_ASCII);
        field.setIndexes(0, 1)

        expect:
        field.charValue == 'A' as char
    }

    def "should reset state"() {
        when:
        field.reset()

        then:
        field.@fieldValue.charValue == Character.MIN_VALUE
    }

    def "should set value"() {
        when:
        field.charValue = 'B' as char

        then:
        field.@fieldValue.charValue == 'B' as char
        field.charValue == 'B' as char
        field.@fieldValue.charValueRaw == AsciiString.c2b('B' as char)
        field.valueSet
        underlyingBuf.readerIndex(5).toString(StandardCharsets.US_ASCII) == "A"
    }

    def "should append provided byte buf with value"() {
        setup:
        field.charValue = 'Z' as char
        def buf = Unpooled.buffer(1, 1)

        when:
        def result = field.appendByteBufWithValue(buf, TestSpec.INSTANCE)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "Z"
        result == AsciiString.c2b('Z' as char)
    }

    def "should copy unparsed value from other field"() {
        setup:
        Field newField = new Field(5, new FieldCodec())

        when:
        newField.copyDataFrom(field)

        then:
        newField.@fieldValue.charValue == FieldValue.CHAR_DEFAULT_VALUE
        newField.@fieldValue.charValueRaw == FieldValue.CHAR_DEFAULT_VALUE
        newField.charValue == 'A' as char
        newField.startIndex == 5
        newField.endIndex == 6
        newField.indicesSet
        newField.valueSet
        newField.fieldData.is(field.fieldData)
    }

    def "should copy previously set value"() {
        setup:
        Field existingField = new Field(10, new FieldCodec())
        existingField.charValue = 'Z'
        Field newField = new Field(11, new FieldCodec())

        when:
        newField.copyDataFrom(existingField)

        then:
        newField.@fieldValue.charValue == 'Z' as char
        newField.@fieldValue.charValueRaw == AsciiString.c2b('Z' as char)
        newField.@fieldValue.length == 1
        newField.startIndex == 0
        newField.endIndex == 0
        !newField.indicesSet
        newField.valueSet
        newField.fieldData == null
    }
}
