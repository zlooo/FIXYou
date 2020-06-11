package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.model.FieldType
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class AbstractFieldTest extends Specification {

    private static final byte a_IN_ASCII = 141
    private AbstractField field = new TestField(1)

    def "should encode field number on creation"() {
        expect:
        field.encodedFieldNumber.toString(StandardCharsets.US_ASCII) == "1="
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
        field.encodedFieldNumber.readerIndex(2)

        when:
        field.reset()

        then:
        field.encodedFieldNumber.readerIndex() == 0
        field.startIndex == 0
        field.endIndex == 0
        !field.valueSet
    }

    def "should release field data when closed"() {
        setup:
        field.setFieldData(Unpooled.buffer(1, 1))

        when:
        field.close()

        then:
        field.encodedFieldNumber.refCnt() == 0
        field.fieldData.refCnt() == 1
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
        void appendByteBufWithValue(ByteBuf out) {

        }
    }
}
