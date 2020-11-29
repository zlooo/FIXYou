package io.github.zlooo.fixyou.parser.model


import spock.lang.Specification

import java.nio.charset.StandardCharsets

class FieldTest extends Specification {

    private Field field = new Field(1, new FieldCodec())

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
