package io.github.zlooo.fixyou.model

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.TestSpec
import org.assertj.core.api.Assertions
import org.assertj.core.data.MapEntry
import spock.lang.Specification

class DefaultExtendedFixSpecTest extends Specification {

    private DefaultExtendedFixSpec fixSpec = new DefaultExtendedFixSpec(TestSpec.INSTANCE)

    def "should create number to type maps"() {
        when:
        fixSpec = new DefaultExtendedFixSpec(TestSpec.INSTANCE)

        then:
        Assertions.assertThat(fixSpec.headerFieldNumbers.collect {
            Assertions.entry(it.key, it.value)
        }).containsExactlyInAnyOrder(MapEntry.entry(FixConstants.BEGIN_STRING_FIELD_NUMBER, 0 as byte), MapEntry.entry(FixConstants.BODY_LENGTH_FIELD_NUMBER, 0 as byte))
        Assertions.assertThat(fixSpec.fieldNumberToType.collect {
            Assertions.entry(it.key, it.value)
        }).containsExactlyInAnyOrder(MapEntry.entry(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, FieldType.LONG), MapEntry.entry(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, FieldType.LONG), MapEntry.entry(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER, FieldType.LONG), MapEntry.entry(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, FieldType.LONG))
    }

    def "should get header fields order"() {
        expect:
        fixSpec.headerFieldsOrder == TestSpec.INSTANCE.headerFieldsOrder
    }

    def "should get header field types"() {
        expect:
        fixSpec.headerFieldTypes == TestSpec.INSTANCE.headerFieldTypes
    }

    def "should get body fields order"() {
        expect:
        fixSpec.bodyFieldsOrder == TestSpec.INSTANCE.bodyFieldsOrder
    }

    def "should get body field types"() {
        expect:
        fixSpec.bodyFieldTypes == TestSpec.INSTANCE.bodyFieldTypes
    }

    def "should get message types"() {
        expect:
        fixSpec.messageTypes == TestSpec.INSTANCE.messageTypes
    }

    def "should get application version id"() {
        expect:
        fixSpec.applicationVersionId() == TestSpec.INSTANCE.applicationVersionId()
    }

    def "should get repeating group field numbers"() {
        expect:
        fixSpec.getRepeatingGroupFieldNumbers(1) == TestSpec.INSTANCE.getRepeatingGroupFieldNumbers(1)
    }
}
