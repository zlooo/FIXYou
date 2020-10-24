package io.github.zlooo.fixyou.parser.model


import io.github.zlooo.fixyou.parser.TestSpec
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class FieldTest extends Specification {

    private Field field = new Field(1, TestSpec.INSTANCE, new FieldCodec())

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
}
