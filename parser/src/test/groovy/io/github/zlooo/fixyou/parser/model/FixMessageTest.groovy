package io.github.zlooo.fixyou.parser.model


import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.parser.TestSpec
import spock.lang.Specification

class FixMessageTest extends Specification {

    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)

    def "should set new message byte source"() {
        setup:
        def oldMessageByteSource = Mock(ByteBufComposer)
        def newMessageByteSource = Mock(ByteBufComposer)
        fixMessage.@messageByteSource = oldMessageByteSource

        when:
        fixMessage.setMessageByteSource(newMessageByteSource)

        then:
        fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).fieldData == newMessageByteSource
        0 * _
    }

    def "should reset all data fields and release message byte source"() {
        setup:
        def longField = fixMessage.getField(TestSpec.LONG_FIELD_NUMBER)
        longField.value = 666L
        longField.startIndex = 7
        longField.endIndex = 10
        def booleanField = fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER)
        booleanField.value = true
        booleanField.startIndex = 12
        booleanField.endIndex = 13
        def messageByteSource = Mock(ByteBufComposer)
        fixMessage.@messageByteSource = messageByteSource
        fixMessage.startIndex = 1
        fixMessage.endIndex = 2

        when:
        fixMessage.resetAllDataFieldsAndReleaseByteSource()

        then:
        !longField.isValueSet()
        !booleanField.isValueSet()
        1 * messageByteSource.releaseData(1, 2)
        fixMessage.@messageByteSource == null
        fixMessage.startIndex == 0
        fixMessage.endIndex == 0
    }

    def "should close fields when message is closed"() {
        def field = Mock(AbstractField)
        def fieldsOrdered = fixMessage.fieldsOrdered
        (0..fieldsOrdered.length - 1).forEach { fieldsOrdered[it] = field }

        when:
        fixMessage.close()

        then:
        fieldsOrdered.length * field.close()
        0 * _
    }
}
