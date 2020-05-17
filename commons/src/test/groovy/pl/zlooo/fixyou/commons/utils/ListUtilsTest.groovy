package pl.zlooo.fixyou.commons.utils

import org.assertj.core.api.Assertions
import spock.lang.Specification

class ListUtilsTest extends Specification {

    def "should create list containing passed elements"() {
        setup:
        def element1 = "element1"
        def element2 = "element2"
        def element3 = "element3"

        when:
        def result = ListUtils.of(element1, element2, element3)

        then:
        Assertions.assertThat(result).containsExactly(element1, element2, element3)
    }

    def "should create unmodifiable list"() {
        when:
        ListUtils.of().add("element")

        then:
        thrown(UnsupportedOperationException)
    }
}
