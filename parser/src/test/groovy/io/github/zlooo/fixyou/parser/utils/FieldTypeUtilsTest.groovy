package io.github.zlooo.fixyou.parser.utils

import io.github.zlooo.fixyou.model.FieldType
import io.github.zlooo.fixyou.parser.TestSpec
import spock.lang.Specification

class FieldTypeUtilsTest extends Specification {

    def "should create every field type"() {
        expect:
        FieldTypeUtils.createField(fieldType, fieldNumber, TestSpec.INSTANCE) != null

        where:
        fieldType << FieldType.values()
        fieldNumber = fieldType == FieldType.GROUP ? TestSpec.USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER : TestSpec.LONG_FIELD_NUMBER
    }

    def "should not create group field when child spec is empty or null"() {
        when:
        FieldTypeUtils.createField(FieldType.GROUP, fieldNumber, TestSpec.INSTANCE)

        then:
        thrown(IllegalArgumentException)

        where:
        fieldNumber << [TestSpec.NULL_CHILD_PAIR_SPEC_FIELD_NUMBER, TestSpec.EMPTY_CHILD_PAIR_SPEC_FIELD_NUMBER]
    }
}
