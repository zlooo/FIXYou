package pl.zlooo.fixyou.parser.model

import pl.zlooo.fixyou.parser.TestSpec
import spock.lang.Specification

class FixMessageTest extends Specification {

    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)

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

    def "should reset all but excludeddata fields"() {
        setup:
        fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).value = 666L
        fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).value = true

        when:
        fixMessage.resetDataFields(TestSpec.BOOLEAN_FIELD_NUMBER)

        then:
        !fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).isValueSet()
        fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).isValueSet()
    }

    def "should reset all data fields before deallocation"() {
        setup:
        fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).value = 666L
        fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).value = true

        when:
        fixMessage.deallocate()

        then:
        !fixMessage.getField(TestSpec.LONG_FIELD_NUMBER).isValueSet()
        !fixMessage.getField(TestSpec.BOOLEAN_FIELD_NUMBER).isValueSet()
    }
}
