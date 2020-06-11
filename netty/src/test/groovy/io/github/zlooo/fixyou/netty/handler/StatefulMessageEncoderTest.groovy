package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.parser.model.GroupField
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class StatefulMessageEncoderTest extends Specification {

    private static final int CHECK_SUM_MODULO = 256
    private StatefulMessageEncoder messageEncoder = new StatefulMessageEncoder()
    private FixMessage fixMessage = new FixMessage(TestSpec.INSTANCE)
    private ChannelHandlerContext channelHandlerContext = Mock()

    def "should encode simple message"() {
        setup:
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).value = "FIXT.1.1".toCharArray()
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = "sender".toCharArray()
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = "target".toCharArray()
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value = "test".toCharArray()
        fixMessage.getField(FixConstants.CHECK_SUM_FIELD_NUMBER).value = 1
        ByteBuf buf = Unpooled.buffer(10000)

        when:
        messageEncoder.encode(channelHandlerContext, fixMessage, buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == expectedBuffer("49=sender\u000156=target\u000158=test\u0001").toString(StandardCharsets.US_ASCII)
    }

    def "should encode message containing repeating group"() {
        setup:
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).value = "FIXT.1.1".toCharArray()
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = "sender".toCharArray()
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = "target".toCharArray()
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).value = "test".toCharArray()
        def groupField = fixMessage.<GroupField> getField(453)
        groupField.getFieldForCurrentRepetition(448).value = "partyID".toCharArray()
        groupField.next()
        ByteBuf buf = Unpooled.buffer(10000)

        when:
        messageEncoder.encode(channelHandlerContext, fixMessage, buf)

        then:
        buf.toString(StandardCharsets.US_ASCII) == expectedBuffer("49=sender\u000156=target\u000158=test\u0001453=1\u0001448=partyID\u0001").toString(StandardCharsets.US_ASCII)
    }

    def "should reset processor when handler is reset"() {
        setup:
        messageEncoder.getValueAddingByteProcessor().process(1 as byte)

        when:
        messageEncoder.reset()

        then:
        messageEncoder.getValueAddingByteProcessor().getResult() == 0
    }

    ByteBuf expectedBuffer(String body) {
        String fixMessage = "8=FIXT.1.1\u00019=${body.length()}\u0001${body}"
        def checksum = fixMessage.getBytes(StandardCharsets.US_ASCII).collect { it as int }.sum() % CHECK_SUM_MODULO
        fixMessage += "10=${checksum.toString().padLeft(3, '0')}\u0001"
        def buffer = Unpooled.copiedBuffer(fixMessage, 0, fixMessage.length(), StandardCharsets.US_ASCII)
        return buffer
    }
}
