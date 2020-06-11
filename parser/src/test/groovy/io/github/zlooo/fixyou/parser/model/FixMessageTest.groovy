package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.parser.TestSpec
import io.netty.buffer.ByteBuf
import org.assertj.core.api.Assertions
import spock.lang.Specification

class FixMessageTest extends Specification {

    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)

    def "should set new message byte source"() {
        setup:
        def oldMessageByteSource = Mock(ByteBuf)
        def newMessageByteSource = Mock(ByteBuf)
        fixMessage.@messageByteSource = oldMessageByteSource

        when:
        fixMessage.setMessageByteSourceAndRetain(newMessageByteSource)

        then:
        1 * newMessageByteSource.retain()
        1 * oldMessageByteSource.release()
        fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).fieldData == newMessageByteSource
        0 * _
    }

    def "should set first message byte source"() {
        setup:
        def newMessageByteSource = Mock(ByteBuf)

        when:
        fixMessage.setMessageByteSourceAndRetain(newMessageByteSource)

        then:
        1 * newMessageByteSource.retain()
        fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).fieldData == newMessageByteSource
        0 * _
    }

    def "should not set new message byte source when new and old are the same buffer"() {
        setup:
        def messageByteSource = Mock(ByteBuf)
        fixMessage.@messageByteSource = messageByteSource

        when:
        fixMessage.setMessageByteSourceAndRetain(messageByteSource)

        then:
        fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).fieldData == null
        0 * _
    }

    def "should reset all data fields"() {
        setup:
        fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).value = 666L
        fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).value = true

        when:
        fixMessage.resetAllDataFields()

        then:
        !fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).isValueSet()
        !fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).isValueSet()
    }

    def "should reset all but excluded data fields"() {
        setup:
        fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).value = 666L
        fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).value = true

        when:
        fixMessage.resetDataFields(TestSpec.BOOLEAN_FIELD_NUMBER)

        then:
        !fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).isValueSet()
        fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).isValueSet()
    }

    def "should reset all data fields and release message byte source before deallocation"() {
        setup:
        fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).value = 666L
        fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).value = true
        def messageByteSource = Mock(ByteBuf)
        fixMessage.@messageByteSource = messageByteSource

        when:
        fixMessage.deallocate()

        then:
        !fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).isValueSet()
        !fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).isValueSet()
        1 * messageByteSource.release()
        fixMessage.@messageByteSource == null
    }

    def "should close fields when message is closed"() {
        when:
        fixMessage.close()

        then:
        Assertions.assertThat(fixMessage.getFieldsOrdered()).allMatch({ field -> field.encodedFieldNumber.refCnt() == 0 })
    }
}
