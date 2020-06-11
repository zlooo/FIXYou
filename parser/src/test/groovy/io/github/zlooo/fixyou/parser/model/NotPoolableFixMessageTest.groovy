package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.parser.TestSpec
import io.netty.buffer.ByteBuf
import org.assertj.core.api.Assertions
import spock.lang.Specification

class NotPoolableFixMessageTest extends Specification {

    private NotPoolableFixMessage fixMessage = new NotPoolableFixMessage(TestSpec.INSTANCE)

    def "should close message and release buffer on deallocate"() {
        setup:
        def messageByteSource = Mock(ByteBuf)
        fixMessage.messageByteSource = messageByteSource

        when:
        fixMessage.deallocate()

        then:
        Assertions.assertThat(fixMessage.fieldsOrdered).allMatch({ isFieldClosed(it) })
        1 * messageByteSource.release()
        0 * _
    }

    def "should close message buffer on deallocate"() {
        when:
        fixMessage.deallocate()

        then:
        Assertions.assertThat(fixMessage.fieldsOrdered).allMatch({ isFieldClosed(it) })
        0 * _
    }

    private static boolean isFieldClosed(AbstractField field) {
        return field.encodedFieldNumber.refCnt() == 0
    }
}
