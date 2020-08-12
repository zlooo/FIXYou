package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.parser.TestSpec
import spock.lang.Specification

class NotPoolableFixMessageTest extends Specification {

    private NotPoolableFixMessage fixMessage = new NotPoolableFixMessage(TestSpec.INSTANCE)
    private AbstractField field = Mock()

    void setup() {
        def fieldsOrdered = fixMessage.fieldsOrdered
        (0..fieldsOrdered.length - 1).forEach { fieldsOrdered[it] = field }
    }

    def "should close message and release buffer on deallocate"() {
        setup:
        def messageByteSource = Mock(ByteBufComposer)
        fixMessage.messageByteSource = messageByteSource

        when:
        fixMessage.deallocate()

        then:
        fixMessage.fieldsOrdered.length * field.close()
        fixMessage.fieldsOrdered.length * field.getEndIndex() >> 10
        1 * messageByteSource.releaseDataUpTo(11)
        0 * _
    }

    def "should close message buffer on deallocate"() {
        when:
        fixMessage.deallocate()

        then:
        fixMessage.fieldsOrdered.length * field.close()
        0 * _
    }
}
