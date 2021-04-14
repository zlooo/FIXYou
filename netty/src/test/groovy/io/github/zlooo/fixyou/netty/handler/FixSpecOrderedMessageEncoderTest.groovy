package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.admin.TestSpec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class FixSpecOrderedMessageEncoderTest extends Specification {

    private FixSpecOrderedMessageEncoder messageEncoder = new FixSpecOrderedMessageEncoder()
    @AutoCleanup
    private FixMessage fixMessage = new NotPoolableFixMessage()
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionAttribute = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig(), new SessionID("testBeginString", "testSender", "testTarget"), TestSpec.INSTANCE)

    def "should encode simple message"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, "FIXT.1.1")
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, "sender")
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, "target")
        fixMessage.setLongValue(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER, 666)
        fixMessage.setBooleanValue(FixConstants.GAP_FILL_FLAG_FIELD_NUMBER, true)
        fixMessage.setCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER, "test")
        fixMessage.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, 1631777144694L)
        fixMessage.setCharValue(TestSpec.TEST_CHAR_FIELD_NUMBER, "1" as char)
        fixMessage.setDoubleValue(TestSpec.TEST_DOUBLE_FIELD_NUMBER, 123L, 2 as short)
        ByteBuf buf = Unpooled.buffer(10000)

        when:
        messageEncoder.encode(channelHandlerContext, fixMessage, buf)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.get() >> sessionState
        buf.toString(StandardCharsets.US_ASCII) == expectedBuffer("49=sender\u000156=target\u000152=20210916-07:25:44.694\u000145=666\u0001123=Y\u000158=test\u00015001=1.23\u00015002=1\u0001").toString(StandardCharsets.US_ASCII)
    }

    def "should encode message containing repeating group"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, "FIXT.1.1")
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, "sender")
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, "target")
        fixMessage.setCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER, "test")
        fixMessage.setCharSequenceValue(448, 453, 0 as byte, 0 as byte, "partyID1")
        fixMessage.setCharSequenceValue(448, 453, 1 as byte, 0 as byte, "partyID2")
        ByteBuf buf = Unpooled.buffer(10000)

        when:
        messageEncoder.encode(channelHandlerContext, fixMessage, buf)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.get() >> sessionState
        buf.toString(StandardCharsets.US_ASCII) == expectedBuffer("49=sender\u000156=target\u000158=test\u0001453=2\u0001448=partyID1\u0001448=partyID2\u0001").toString(StandardCharsets.US_ASCII)
    }

    def "should encode message containing nested repeating group"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, "FIXT.1.1")
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, "sender")
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, "target")
        fixMessage.setCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER, "test")
        fixMessage.setCharValue(787, 85, 0 as byte, 0 as byte, "C" as char)
        fixMessage.setCharSequenceValue(782, 781, 0 as byte, 0 as byte, "ID1")
        fixMessage.setCharSequenceValue(782, 781, 1 as byte, 0 as byte, "ID2")
        fixMessage.setCharValue(787, 85, 1 as byte, 0 as byte, "D" as char)
        fixMessage.setCharSequenceValue(782, 781, 0 as byte, 1 as byte, "ID3")
        fixMessage.setCharSequenceValue(782, 781, 1 as byte, 1 as byte, "ID4")
        ByteBuf buf = Unpooled.buffer(10000)

        when:
        messageEncoder.encode(channelHandlerContext, fixMessage, buf)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.get() >> sessionState
        buf.toString(StandardCharsets.US_ASCII) ==
        expectedBuffer("49=sender\u000156=target\u000158=test\u000185=2\u0001787=C\u0001781=2\u0001782=ID1\u0001782=ID2\u0001787=D\u0001781=2\u0001782=ID3\u0001782=ID4\u0001").toString(StandardCharsets.US_ASCII)
    }

    ByteBuf expectedBuffer(String body) {
        def wholeBody = "35=D\u0001" + body
        String fixMessage = "8=FIXT.1.1\u00019=${wholeBody.length()}\u0001${wholeBody}"
        def checksum = fixMessage.getBytes(StandardCharsets.US_ASCII).collect { it as int }.sum() % 256
        fixMessage += "10=${checksum.toString().padLeft(3, '0')}\u0001"
        return Unpooled.copiedBuffer(fixMessage, 0, fixMessage.length(), StandardCharsets.US_ASCII)
    }
}
