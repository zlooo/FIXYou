package io.github.zlooo.fixyou.parser

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage
import io.netty.buffer.Unpooled
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class FixMessageParserTest extends Specification {

    private static final FixSpec50SP2 fixSpec50SP2 = new FixSpec50SP2()
    @AutoCleanup
    private FixMessage fixMessage = new NotPoolableFixMessage()
    private ByteBufComposer byteBufComposer = new ByteBufComposer(10)
    private FixMessageParser fixMessageParser = new FixMessageParser(byteBufComposer, fixSpec50SP2, fixMessage)

    void cleanup() {
        byteBufComposer.reset()
    }

    def "should start parsing"() {
        setup:
        fixMessageParser.@storedEndIndexOfLastUnfinishedMessage = 666

        when:
        fixMessageParser.startParsing()

        then:
        fixMessageParser.@storedEndIndexOfLastUnfinishedMessage == 0
    }

    def "should parse new order single message, simple message case"() {
        setup:
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(simpleNewOrderSingle.getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageParser.startParsing()
        fixMessageParser.parseFixMsgBytes()

        then:
        fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        assertSimpleNewOrderSingle(fixMessage)
    }

    def "should discard garbage data from fix message"() {
        setup:
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(("garbage garbage" + simpleNewOrderSingle).getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageParser.startParsing()
        fixMessageParser.parseFixMsgBytes()

        then:
        fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        assertSimpleNewOrderSingle(fixMessage)
    }

    def "should parse execution report message, message with repeating group(382)"() {
        setup:
        String message = "8=FIX.4.2\u00019=378\u000135=8\u0001128=XYZ\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20210323-15:40:35\u000155=CVS\u000137=NF 0542/03232009\u000111=NF 0542/03232009\u000117=NF 0542/03232009001001001" +
                         "\u000120=0\u000139=2\u0001150=2\u000154=1\u000138=100\u000140=1\u000159=0\u000131=25.4800\u000132=100\u000114=0\u00016=0\u0001151=0\u000160=20210323-15:40:30\u000158=Fill\u000130=N\u000176=0034\u0001207=N\u0001" +
                         "47=A\u0001382=1\u0001375=TOD\u0001337=0000\u0001437=100\u0001438=20090330-23:40:35\u000129=1\u000163=0\u000110=080\u0001"
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageParser.startParsing()
        fixMessageParser.parseFixMsgBytes()

        then:
        fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        fixMessage.getCharSequenceValue(8).toString() == "FIX.4.2"
        fixMessage.getLongValue(9) == 378
        fixMessage.getCharSequenceValue(35).toString() == "8"
        fixMessage.getLongValue(34) == 5
        fixMessage.getCharSequenceValue(49).toString() == "CCG"
        fixMessage.getCharSequenceValue(56).toString() == "ABC_DEFG01"
        fixMessage.getTimestampValue(52) == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20210323-15:40:35", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getCharSequenceValue(55).toString() == "CVS"
        fixMessage.getCharSequenceValue(37).toString() == "NF 0542/03232009"
        fixMessage.getCharSequenceValue(11).toString() == "NF 0542/03232009"
        fixMessage.getCharSequenceValue(17).toString() == "NF 0542/03232009001001001"
        fixMessage.getCharValue(39) == "2" as char
        fixMessage.getCharValue(150) == "2" as char
        fixMessage.getCharValue(54) == "1" as char
        fixMessage.getDoubleUnscaledValue(38) == 100
        fixMessage.getScale(38) == 0 as short
        fixMessage.getCharValue(40) == "1" as char
        fixMessage.getCharValue(59) == "0" as char
        fixMessage.getDoubleUnscaledValue(31) == 254800
        fixMessage.getScale(31) == 4 as short
        fixMessage.getDoubleUnscaledValue(32) == 100
        fixMessage.getScale(32) == 0 as short
        fixMessage.getDoubleUnscaledValue(14) == 0
        fixMessage.getScale(14) == 0 as short
        fixMessage.getDoubleUnscaledValue(6) == 0
        fixMessage.getScale(6) == 0 as short
        fixMessage.getDoubleUnscaledValue(151) == 0
        fixMessage.getScale(151) == 0 as short
        fixMessage.getTimestampValue(60) == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20210323-15:40:30", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getCharSequenceValue(58).toString() == "Fill"
        fixMessage.getCharSequenceValue(30).toString() == "N"
        fixMessage.getCharSequenceValue(207).toString() == "N"
        fixMessage.getLongValue(382) == 1
        fixMessage.getCharSequenceValue(375, 382, 0 as byte, 0 as byte).toString() == "TOD"
        fixMessage.getCharSequenceValue(337, 382, 0 as byte, 0 as byte).toString() == "0000"
        fixMessage.getDoubleUnscaledValue(437, 382, 0 as byte, 0 as byte) == 100
        fixMessage.getScale(437, 382, 0 as byte, 0 as byte) == 0
        fixMessage.getTimestampValue(438, 382, 0 as byte, 0 as byte) == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20210330-23:40:35", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getCharValue(29) == "1" as char
        fixMessage.getCharSequenceValue(63).toString() == "0"
        fixMessage.getCharSequenceValue(10).toString() == "080"
    }

    def "should parse execution report message, message with multiple occurrences of repeating group(382)"() {
        setup:
        String message = "8=FIX.4.2\u00019=378\u000135=8\u0001128=XYZ\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20210323-15:40:35\u000155=CVS\u000137=NF 0542/03232009\u000111=NF 0542/03232009\u000117=NF 0542/03232009001001001" +
                         "\u000120=0\u000139=2\u0001150=2\u000154=1\u000138=100\u000140=1\u000159=0\u000131=25.4800\u000132=100\u000114=0\u00016=0\u0001151=0\u000160=20210323-15:40:30\u000158=Fill\u000130=N\u000176=0034\u0001207=N\u0001" +
                         "47=A\u0001382=2\u0001375=TOD\u0001337=0000\u0001437=100\u0001438=20090330-23:40:35\u0001375=TOD2\u0001337=0001\u0001437=101\u0001438=20090330-23:40:36\u000129=1\u000163=0\u000110=080\u0001"
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageParser.startParsing()
        fixMessageParser.parseFixMsgBytes()

        then:
        fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        fixMessage.getCharSequenceValue(8).toString() == "FIX.4.2"
        fixMessage.getLongValue(9) == 378
        fixMessage.getCharSequenceValue(35).toString() == "8"
        fixMessage.getLongValue(34) == 5
        fixMessage.getCharSequenceValue(49).toString() == "CCG"
        fixMessage.getCharSequenceValue(56).toString() == "ABC_DEFG01"
        fixMessage.getTimestampValue(52) == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20210323-15:40:35", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getCharSequenceValue(55).toString() == "CVS"
        fixMessage.getCharSequenceValue(37).toString() == "NF 0542/03232009"
        fixMessage.getCharSequenceValue(11).toString() == "NF 0542/03232009"
        fixMessage.getCharSequenceValue(17).toString() == "NF 0542/03232009001001001"
        fixMessage.getCharValue(39) == "2" as char
        fixMessage.getCharValue(150) == "2" as char
        fixMessage.getCharValue(54) == "1" as char
        fixMessage.getDoubleUnscaledValue(38) == 100
        fixMessage.getScale(38) == 0
        fixMessage.getCharValue(40) == "1" as char
        fixMessage.getCharValue(59) == "0" as char
        fixMessage.getDoubleUnscaledValue(31) == 254800
        fixMessage.getScale(31) == 4
        fixMessage.getDoubleUnscaledValue(32) == 100
        fixMessage.getScale(32) == 0
        fixMessage.getDoubleUnscaledValue(14) == 0
        fixMessage.getScale(14) == 0
        fixMessage.getDoubleUnscaledValue(6) == 0
        fixMessage.getScale(6) == 0
        fixMessage.getDoubleUnscaledValue(151) == 0
        fixMessage.getScale(151) == 0
        fixMessage.getTimestampValue(60) == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20210323-15:40:30", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getCharSequenceValue(58).toString() == "Fill"
        fixMessage.getCharSequenceValue(30).toString() == "N"
        fixMessage.getCharSequenceValue(207).toString() == "N"
        fixMessage.getLongValue(382) == 2
        fixMessage.getCharSequenceValue(375, 382, 0 as byte, 0 as byte).toString() == "TOD"
        fixMessage.getCharSequenceValue(337, 382, 0 as byte, 0 as byte).toString() == "0000"
        fixMessage.getDoubleUnscaledValue(437, 382, 0 as byte, 0 as byte) == 100
        fixMessage.getScale(437, 382, 0 as byte, 0 as byte) == 0
        fixMessage.getTimestampValue(438, 382, 0 as byte, 0 as byte) == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20210330-23:40:35", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getCharSequenceValue(375, 382, 1 as byte, 0 as byte).toString() == "TOD2"
        fixMessage.getCharSequenceValue(337, 382, 1 as byte, 0 as byte).toString() == "0001"
        fixMessage.getDoubleUnscaledValue(437, 382, 1 as byte, 0 as byte) == 101
        fixMessage.getScale(437, 382, 1 as byte, 0 as byte) == 0
        fixMessage.getTimestampValue(438, 382, 1 as byte, 0 as byte) == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20210330-23:40:36", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getCharValue(29) == "1" as char
        fixMessage.getCharSequenceValue(63).toString() == "0"
        fixMessage.getCharSequenceValue(10).toString() == "080"
    }

    def "should parse fake confirmation message, message with nested repeating groups"() {
        setup:
        String message = "8=FIX.4.4\u00019=378\u000135=AK\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20210323-15:40:35\u000185=2\u0001787=C\u0001781=2\u0001782=ID1\u0001782=ID2\u0001787=D\u0001781=2\u0001782=ID3\u0001782=ID4" +
                         "\u000110=080\u0001"
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageParser.startParsing()
        fixMessageParser.parseFixMsgBytes()

        then:
        fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        fixMessage.getCharSequenceValue(8).toString() == "FIX.4.4"
        fixMessage.getLongValue(9) == 378
        fixMessage.getCharSequenceValue(35).toString() == "AK"
        fixMessage.getLongValue(34) == 5
        fixMessage.getCharSequenceValue(49).toString() == "CCG"
        fixMessage.getCharSequenceValue(56).toString() == "ABC_DEFG01"
        fixMessage.getLongValue(52) == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20210323-15:40:35", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getLongValue(85) == 2
        fixMessage.getCharValue(787, 85, 0 as byte, 0 as byte) == 'C' as char
        fixMessage.getLongValue(781, 85, 0 as byte, 0 as byte) == 2
        fixMessage.getCharSequenceValue(782, 781, 0 as byte, 0 as byte).toString() == 'ID1'
        fixMessage.getCharSequenceValue(782, 781, 1 as byte, 0 as byte).toString() == 'ID2'
        fixMessage.getCharValue(787, 85, 1 as byte, 0 as byte) == 'D' as char
        fixMessage.getLongValue(781, 85, 1 as byte, 0 as byte) == 2
        fixMessage.getCharSequenceValue(782, 781, 0 as byte, 1 as byte).toString() == 'ID3'
        fixMessage.getCharSequenceValue(782, 781, 1 as byte, 1 as byte).toString() == 'ID4'
    }

    def "should check if can continue parsing"() {
        setup:
        fixMessageParser.@bytesToParse.readerIndex(readerIndex)
        fixMessageParser.@bytesToParse.@storedEndIndex = storedEndIndex
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage = storedEndIndexOfLastUnfinishedMessage

        expect:
        fixMessageParser.canContinueParsing() == expectedResult

        where:
        readerIndex | storedEndIndex | storedEndIndexOfLastUnfinishedMessage | expectedResult
        0           | 2              | 0                                     | true
        2           | 2              | 0                                     | false
        0           | 2              | 1                                     | true
        2           | 2              | 2                                     | false
    }

    def "should parse unfinished message"() {
        setup:
        String message = "8=FIX.4.4\u00019=378\u000135=AK\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20210323-15:40:"
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageParser.startParsing()
        fixMessageParser.parseFixMsgBytes()

        then:
        !fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == message.length() - 1
        fixMessage.getCharSequenceValue(8).toString() == "FIX.4.4"
        fixMessage.getLongValue(9) == 378
        fixMessage.getCharSequenceValue(35).toString() == "AK"
        fixMessage.getLongValue(34) == 5
        fixMessage.getCharSequenceValue(49).toString() == "CCG"
        fixMessage.getCharSequenceValue(56).toString() == "ABC_DEFG01"
        !fixMessage.isValueSet(52)
        !fixMessageParser.canContinueParsing()
    }

    def "should parse unfinished message that ends with SOH"() {
        setup:
        String message = "8=FIX.4.4\u00019=378\u000135=AK\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20210323-15:40:35\u0001"
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageParser.startParsing()
        fixMessageParser.parseFixMsgBytes()

        then:
        !fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == message.length() - 1
        fixMessage.getCharSequenceValue(8).toString() == "FIX.4.4"
        fixMessage.getLongValue(9) == 378
        fixMessage.getCharSequenceValue(35).toString() == "AK"
        fixMessage.getLongValue(34) == 5
        fixMessage.getCharSequenceValue(49).toString() == "CCG"
        fixMessage.getCharSequenceValue(56).toString() == "ABC_DEFG01"
        fixMessage.getTimestampValue(52) == Instant.parse("2021-03-23T15:40:35Z").toEpochMilli()
        !fixMessageParser.canContinueParsing()
    }

    def "should parse simple new order single fragmented with garbage"() {
        setup:
        def packet1 = Unpooled.wrappedBuffer(simpleNewOrderSingleWithGarbage.substring(0, packet1End).getBytes(StandardCharsets.US_ASCII))
        byteBufComposer.addByteBuf(packet1)
        fixMessageParser.parseFixMsgBytes()
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(simpleNewOrderSingleWithGarbage.substring(packet1End, simpleNewOrderSingleWithGarbage.length()).getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageParser.parseFixMsgBytes()

        then:
        fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        assertSimpleNewOrderSingle(fixMessage)
        fixMessageParser.bytesToParse.readerIndex() == simpleNewOrderSingleWithGarbage.length() - "garbage".length()

        where:
        packet1End | _
        3          | _
        6          | _
        7          | _
        10         | _
        18         | _
    }

    def "should reset parser"() {
        setup:
        fixMessageParser.@storedEndIndexOfLastUnfinishedMessage = 666
        fixMessageParser.@parsingRepeatingGroup = true
        fixMessageParser.@groupIndexNumberStack.addFirst(123)

        when:
        fixMessageParser.reset()

        then:
        fixMessageParser.@bytesToParse.is(byteBufComposer)
        fixMessageParser.@fixMessage.is(fixMessage)
        fixMessageParser.@storedEndIndexOfLastUnfinishedMessage == 0
        !fixMessageParser.@parsingRepeatingGroup
        fixMessageParser.@groupIndexNumberStack.isEmpty()
        fixMessage.refCnt() == 1 //1 because NonPoolableFixMessage does retain() in constructor, basically we're checking that reset() does not release fixMessage
    }

    private final static String simpleNewOrderSingle = "8=FIX.4.2\u00019=146\u000135=D\u000134=4\u000149=ABC_DEFG01\u000152=20210323-15:40:29\u000156=CCG\u0001115=XYZ\u000111=NF " +
                                                       "0542/03232009\u000154=1\u000138=100\u000155=CVS\u000140=1\u000159=0\u000147=A\u000160=20210323-15:40:29\u000121=1\u0001207=N\u000110=139\u0001"

    private final static String simpleNewOrderSingleWithGarbage = "garbae8=FIX.4.2\u00019=146\u000135=D\u000134=4\u000149=ABC_DEFG01\u000152=20210323-15:40:29\u000156=CCG\u0001115=XYZ\u000111=NF " +
                                                                  "0542/03232009\u000154=1\u000138=100\u000155=CVS\u000140=1\u000159=0\u000147=A\u000160=20210323-15:40:29\u000121=1\u0001207=N\u000110=139\u0001garbage"

    private static void assertSimpleNewOrderSingle(FixMessage fixMessage) {
        assert fixMessage.getCharSequenceValue(8).toString() == "FIX.4.2"
        assert fixMessage.getLongValue(9) == 146
        assert fixMessage.getBodyLength() == 146
        assert fixMessage.getCharSequenceValue(35).toString() == "D"
        assert fixMessage.getLongValue(34) == 4
        assert fixMessage.getCharSequenceValue(49).toString() == "ABC_DEFG01"
        assert fixMessage.getTimestampValue(52) == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20210323-15:40:29", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        assert fixMessage.getCharSequenceValue(56).toString() == "CCG"
        assert fixMessage.getCharSequenceValue(115).toString() == "XYZ"
        assert fixMessage.getCharSequenceValue(11).toString() == "NF 0542/03232009"
        assert fixMessage.getCharValue(54) == "1" as char
        assert fixMessage.getDoubleUnscaledValue(38) == 100
        assert fixMessage.getScale(38) == 0 as short
        assert fixMessage.getCharSequenceValue(55).toString() == "CVS"
        assert fixMessage.getCharValue(40) == "1" as char
        assert fixMessage.getCharValue(59) == "0" as char
        assert fixMessage.getTimestampValue(60) == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20210323-15:40:29", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        assert fixMessage.getCharValue(21) == "1" as char
        assert fixMessage.getCharSequenceValue(207).toString() == "N"
        assert fixMessage.getCharSequenceValue(10).toString() == "139"
    }
}
