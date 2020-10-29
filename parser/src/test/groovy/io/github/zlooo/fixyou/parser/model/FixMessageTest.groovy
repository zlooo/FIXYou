package io.github.zlooo.fixyou.parser.model


import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.parser.TestSpec
import spock.lang.Specification

class FixMessageTest extends Specification {

    private FixMessage fixMessage = new FixMessage(new FieldCodec())

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
        longField.longValue = 666L
        longField.startIndex = 7
        longField.endIndex = 10
        def booleanField = fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER)
        booleanField.booleanValue = true
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
        fixMessage.startIndex == FixMessage.NOT_SET
        fixMessage.endIndex == FixMessage.NOT_SET
    }

    def "should close fields when message is closed"() {
        def field = Mock(Field)
        (0..9).forEach {
            fixMessage.getField(it + 1)
            fixMessage.actualFields[it] = field
        }

        when:
        fixMessage.close()

        then:
        fixMessage.@actualFieldsLength * field.close()
        0 * _
    }

    def "should extend fields arrays when necessary"() {
        when:
        def result = fixMessage.getField(5000)

        then:
        result != null
        !result.is(FixMessage.PLACEHOLDER)
        result instanceof Field
        result.number == 5000
        !result.valueSet
        fixMessage.allFields.length >= 5001
        fixMessage.actualFields.length >= 5001
    }

    def "should copy values to other message"() {
        setup:
        def longField = fixMessage.getField(TestSpec.LONG_FIELD_NUMBER)
        longField.longValue = 666L
        longField.startIndex = 7
        longField.endIndex = 10
        def booleanField = fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER)
        booleanField.booleanValue = true
        booleanField.startIndex = 12
        booleanField.endIndex = 13
        def messageByteSource = Mock(ByteBufComposer)
        fixMessage.@messageByteSource = messageByteSource
        fixMessage.startIndex = 1
        fixMessage.endIndex = 2
        def newFixMessage = new FixMessage(new FieldCodec())

        when:
        newFixMessage.copyDataFrom(fixMessage, unsetIndices)

        then:
        newFixMessage.getField(TestSpec.LONG_FIELD_NUMBER).longValue == 666
        newFixMessage.getField(TestSpec.LONG_FIELD_NUMBER).valueSet
        newFixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).booleanValue
        newFixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).valueSet
        newFixMessage.messageByteSource.is(messageByteSource)
        newFixMessage.startIndex == 1
        newFixMessage.endIndex == 2
        fixMessage.startIndex == expectedSrcStartIndex
        fixMessage.endIndex == expectedSrcEndIndex

        where:
        unsetIndices | expectedSrcStartIndex | expectedSrcEndIndex
        false        | 1                  | 2
        true         | FixMessage.NOT_SET | FixMessage.NOT_SET
    }
}
