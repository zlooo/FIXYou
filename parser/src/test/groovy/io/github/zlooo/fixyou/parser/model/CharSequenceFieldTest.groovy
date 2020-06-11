package io.github.zlooo.fixyou.parser.model


import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class CharSequenceFieldTest extends Specification {

    private CharSequenceField field

    void setup() {
        field = new CharSequenceField(1);
        field.fieldData = Unpooled.buffer(30, 30)
        field.fieldData.writerIndex(1)
        field.fieldData.writeCharSequence("test", StandardCharsets.US_ASCII)
        field.setIndexes(1, 5)
    }

    def "should get value"() {
        expect:
        field.getValue().toString() == "test"
    }

    def "should get default value when value is not set"() {
        setup:
        field.reset()

        expect:
        field.value != null
        field.value.length == 0
    }

    def "should cache value once parsed"() {
        setup:
        field.getValue()
        field.fieldData.clear().writeCharSequence("valueThatShouldBeIgnored", StandardCharsets.US_ASCII)

        expect:
        field.getValue().toString() == "test"
    }

    def "should reset state"() {
        when:
        field.resetInnerState()

        then:
        field.@length == 0
        field.@returnValue.length() == 0
    }

    def "should set value from char array"() {
        when:
        field.setValue("someVeryLongValue".toCharArray())

        then:
        field.@value.toString() == "someVeryLongValue"
        field.length == "someVeryLongValue".length()
        field.valueSet
    }

    def "should set value from char sequence"() {
        setup:
        CharSequenceField field2 = new CharSequenceField(2)
        field2.setValue("someVeryLongValue".toCharArray(), "someVeryLongValue".length())

        when:
        field.setValue(field2)

        then:
        field.@value.toString() == "someVeryLongValue"
        field.length == "someVeryLongValue".length()
        field.valueSet
    }

    def "should set value from char array with limited length"() {
        when:
        field.setValue("someVeryLongValue".toCharArray(), 4)

        then:
        field.@value.toString() == "some"
        field.length == 4
        field.valueSet
    }

    def "should append provided byte buf with value"() {
        setup:
        field.value = "textToWrite".toCharArray()
        def buf = Unpooled.buffer(20, 20)

        when:
        field.appendByteBufWithValue(buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == "textToWrite"
    }
}
