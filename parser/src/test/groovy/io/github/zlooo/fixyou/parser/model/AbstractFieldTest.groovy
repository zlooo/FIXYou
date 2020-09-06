package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.model.FieldType
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class AbstractFieldTest extends Specification {

    private TestField field = new TestField(1)

    def "should encode field number on creation"() {
        expect:
        new String(field.encodedFieldNumber, StandardCharsets.US_ASCII) == "1="
        field.number == 1
    }

    def "should set indexes"() {
        when:
        field.setIndexes(3, 6)

        then:
        field.startIndex == 3
        field.endIndex == 6
        field.valueSet
    }

    def "should reset"() {
        setup:
        field.setIndexes(1, 3)

        when:
        field.reset()

        then:
        field.startIndex == 0
        field.endIndex == 0
        !field.valueSet
    }

    private static class TestField extends AbstractField {

        TestField(int number) {
            super(number)
        }

        @Override
        FieldType getFieldType() {
            return FieldType.CHAR
        }

        @Override
        protected void resetInnerState() {

        }

        @Override
        int appendByteBufWithValue(ByteBuf out) {

        }
    }
}
