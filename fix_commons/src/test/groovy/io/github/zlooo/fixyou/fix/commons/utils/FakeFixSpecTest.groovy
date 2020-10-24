package io.github.zlooo.fixyou.fix.commons.utils

import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.parser.FakeFixSpec
import spock.lang.Specification

class FakeFixSpecTest extends Specification {

    private FakeFixSpec fixSpec = new FakeFixSpec()

    def "should get fields in order"() {
        expect:
        fixSpec.getFieldsOrder().length == 0
    }

    def "should get message types"() {
        expect:
        fixSpec.getMessageTypes().length == 0
    }

    def "should return highest field number"() {
        expect:
        fixSpec.highestFieldNumber() == 0
    }

    def "should return application version id"() {
        expect:
        fixSpec.applicationVersionId() == ApplicationVersionID.FIX50SP2
    }

    def "should return repeating groups config"() {
        expect:
        fixSpec.getRepeatingGroupFieldNumbers(0).length == 0
    }
}
