package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.model.FieldType
import io.netty.buffer.Unpooled
import spock.lang.Specification

class AbstractFieldTest extends Specification {

    private static final byte a_IN_ASCII = 141
    private AbstractField field = new TestField(1, 1)

    def "should set data from supplied byte buf"() {
        when:
        field.setFieldData(Unpooled.buffer(1).writeByte(a_IN_ASCII))

        then:
        field.fieldData.writerIndex() == 1
        field.fieldData.readerIndex() == 0
        field.fieldData.readByte() == a_IN_ASCII
    }

    def "should set data from supplied byte array"() {
        when:
        field.setFieldData([a_IN_ASCII] as byte[])

        then:
        field.fieldData.writerIndex() == 1
        field.fieldData.readerIndex() == 0
        field.fieldData.readByte() == a_IN_ASCII
    }

    def "should release field data when closed"() {
        when:
        field.close()

        then:
        field.fieldData.refCnt() == 0
    }

    private static class TestField extends AbstractField {

        TestField(int number, int fieldDataLength) {
            super(number, fieldDataLength, false)
        }

        @Override
        FieldType getFieldType() {
            return FieldType.CHAR
        }

        @Override
        protected void resetInnerState() {

        }
    }
}
