package io.github.zlooo.fixyou.parser.model

import org.assertj.core.api.Assertions
import spock.lang.Specification

class NotPoolableFixMessageTest extends Specification {

    private NotPoolableFixMessage fixMessage = new NotPoolableFixMessage(io.github.zlooo.fixyou.parser.TestSpec.INSTANCE)

    def "should close message on deallocate"() {
        when:
        fixMessage.deallocate()

        then:
        Assertions.assertThat(fixMessage.fieldsOrdered).allMatch({ isFieldClosed(it) })
    }

    private static boolean isFieldClosed(AbstractField field) {
        return field.encodedFieldNumber.refCnt() == 0 && field.fieldData.refCnt() == 0
    }
}
