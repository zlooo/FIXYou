package io.github.zlooo.fixyou.parser.model


import spock.lang.Specification

class FixMessageTest extends Specification {

    private FixMessage fixMessage = new FixMessage(io.github.zlooo.fixyou.parser.TestSpec.INSTANCE)

    def "should reset all data fields"() {
        setup:
        fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.LONG_FIELD_NUMBER).value = 666L
        fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.BOOLEAN_FIELD_NUMBER).value = true

        when:
        fixMessage.resetAllDataFields()

        then:
        !fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.LONG_FIELD_NUMBER).isValueSet()
        !fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.BOOLEAN_FIELD_NUMBER).isValueSet()
    }

    def "should reset all but excludeddata fields"() {
        setup:
        fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.LONG_FIELD_NUMBER).value = 666L
        fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.BOOLEAN_FIELD_NUMBER).value = true

        when:
        fixMessage.resetDataFields(io.github.zlooo.fixyou.parser.TestSpec.BOOLEAN_FIELD_NUMBER)

        then:
        !fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.LONG_FIELD_NUMBER).isValueSet()
        fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.BOOLEAN_FIELD_NUMBER).isValueSet()
    }

    def "should reset all data fields before deallocation"() {
        setup:
        fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.LONG_FIELD_NUMBER).value = 666L
        fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.BOOLEAN_FIELD_NUMBER).value = true

        when:
        fixMessage.deallocate()

        then:
        !fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.LONG_FIELD_NUMBER).isValueSet()
        !fixMessage.getField(io.github.zlooo.fixyou.parser.TestSpec.BOOLEAN_FIELD_NUMBER).isValueSet()
    }
}
