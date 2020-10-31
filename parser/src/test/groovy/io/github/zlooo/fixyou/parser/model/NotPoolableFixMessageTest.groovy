package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import spock.lang.Specification

class NotPoolableFixMessageTest extends Specification {

    private NotPoolableFixMessage fixMessage = new NotPoolableFixMessage(new FieldCodec())
    private Field field = Mock()

    void setup() {
        (0..9).forEach {
            fixMessage.getField(it + 1)
            fixMessage.actualFields[it] = field
        }
    }

    def "should close message and release buffer on deallocate"() {
        setup:
        def messageByteSource = Mock(ByteBufComposer)
        fixMessage.messageByteSource = messageByteSource

        when:
        fixMessage.deallocate()

        then:
        fixMessage.actualFieldsLength * field.close()
        fixMessage.actualFieldsLength * field.getStartIndex() >> 2
        fixMessage.actualFieldsLength * field.getEndIndex() >> 10
        fixMessage.actualFieldsLength * field.isValueSet() >> true
        1 * messageByteSource.releaseData(0, 10)
        0 * _
    }

    def "should close message buffer on deallocate"() {
        when:
        fixMessage.deallocate()

        then:
        fixMessage.actualFieldsLength * field.close()
        0 * _
    }
}
