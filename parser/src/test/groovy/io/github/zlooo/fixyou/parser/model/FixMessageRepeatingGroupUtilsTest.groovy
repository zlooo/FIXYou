package io.github.zlooo.fixyou.parser.model

import spock.lang.Specification

class FixMessageRepeatingGroupUtilsTest extends Specification {

    def "should create repeating group key"() {
        expect:
        FixMessageRepeatingGroupUtils.repeatingGroupKey(parentGroupRepetitionIndex, groupNumber, repetitionIndex, fieldNumber) == expectedResult

        where:
        parentGroupRepetitionIndex | groupNumber | repetitionIndex | fieldNumber || expectedResult
        0 as byte                  | 16          | 0 as byte       | 17          || 0x0000001000000011
        1 as byte                  | 16          | 1 as byte       | 17          || 0x0100001001000011
        255 as byte                | 0           | 255 as byte     | 1           || 0xFF000000FF000001 as long
    }

    def "should create group index"() {
        expect:
        FixMessageRepeatingGroupUtils.groupIndex(repetitionIndex, groupNumber) == expectedResult

        where:
        repetitionIndex | groupNumber || expectedResult
        0 as byte       | 1           || 0x00000001 as int
        255 as byte     | 1           || 0xff000001 as int
    }
}
