package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.ObjectPool
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class MessageEncoderTest extends Specification {

    private static final int CHECK_SUM_MODULO = 256
    private MessageEncoder messageEncoder = new MessageEncoder()
    private FixMessage fixMessage = new FixMessage(new FieldCodec())
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionAttribute = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig(), new SessionID("testBeginString".toCharArray(), 15, "testSender".toCharArray(), 10, "testTarget".toCharArray(), 10),
                                                                                           Mock(ObjectPool), Mock(ObjectPool), TestSpec.INSTANCE)

    def "should encode simple message"() {
        setup:
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).charSequenceValue = "FIXT.1.1".toCharArray()
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).charSequenceValue = "sender".toCharArray()
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).charSequenceValue = "target".toCharArray()
        fixMessage.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 666
        fixMessage.getField(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER).booleanValue = true
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).charSequenceValue = "test".toCharArray()
        fixMessage.getField(FixConstants.SENDING_TIME_FIELD_NUMBER).timestampValue = 1600241144694
        fixMessage.getField(TestSpec.TEST_CHAR_FIELD_NUMBER).charValue = "1" as char
        fixMessage.getField(TestSpec.TEST_DOUBLE_FIELD_NUMBER).setDoubleValue(123L, 2 as short)
        ByteBuf buf = Unpooled.buffer(10000)

        when:
        messageEncoder.encode(channelHandlerContext, fixMessage, buf)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.get() >> sessionState
        buf.toString(StandardCharsets.US_ASCII) == expectedBuffer("49=sender\u000156=target\u000152=20200916-07:25:44.694\u000145=666\u0001123=Y\u000158=test\u00015001=1.23\u00015002=1\u0001").toString(StandardCharsets.US_ASCII)
    }

    def "should encode message containing repeating group"() {
        setup:
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).charSequenceValue = "FIXT.1.1".toCharArray()
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).charSequenceValue = "sender".toCharArray()
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).charSequenceValue = "target".toCharArray()
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).charSequenceValue = "test".toCharArray()
        def groupField = fixMessage.getField(453)
        groupField.getFieldForCurrentRepetition(448).charSequenceValue = "partyID".toCharArray()
        groupField.endCurrentRepetition()
        ByteBuf buf = Unpooled.buffer(10000)

        when:
        messageEncoder.encode(channelHandlerContext, fixMessage, buf)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.get() >> sessionState
        buf.toString(StandardCharsets.US_ASCII) == expectedBuffer("49=sender\u000156=target\u000158=test\u0001453=1\u0001448=partyID\u0001").toString(StandardCharsets.US_ASCII)
    }

    ByteBuf expectedBuffer(String body) {
        String fixMessage = "8=FIXT.1.1\u00019=${body.length()}\u0001${body}"
        def checksum = fixMessage.getBytes(StandardCharsets.US_ASCII).collect { it as int }.sum() % CHECK_SUM_MODULO
        fixMessage += "10=${checksum.toString().padLeft(3, '0')}\u0001"
        return Unpooled.copiedBuffer(fixMessage, 0, fixMessage.length(), StandardCharsets.US_ASCII)
    }
}
