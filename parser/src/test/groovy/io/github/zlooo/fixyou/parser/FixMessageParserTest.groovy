package io.github.zlooo.fixyou.parser

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.model.FieldType
import io.github.zlooo.fixyou.parser.model.Field
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class FixMessageParserTest extends Specification {

    private static final FixSpec50SP2 fixSpec50SP2 = new FixSpec50SP2()
    private FixMessage fixMessage = new FixMessage(new FieldCodec())
    private ByteBufComposer byteBufComposer = new ByteBufComposer(10)
    private FixMessageParser fixMessageParser = new FixMessageParser(byteBufComposer, fixSpec50SP2)

    void cleanup() {
        byteBufComposer.reset()
    }

    def "should parse new order single message, simple message case"() {
        setup:
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(simpleNewOrderSingle.getBytes(StandardCharsets.US_ASCII)))
        fixMessageParser.setFixMessage(fixMessage)

        when:
        fixMessageParser.parseFixMsgBytes()

        then:
        fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        assertSimpleNewOrderSingle(fixMessage)
        fixMessage.getStartIndex() == 0
        fixMessage.getEndIndex() == simpleNewOrderSingle.length() - 1
    }

    def "should discard garbage data from fix message"() {
        setup:
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(("garbage garbage" + simpleNewOrderSingle).getBytes(StandardCharsets.US_ASCII)))
        fixMessageParser.setFixMessage(fixMessage)

        when:
        fixMessageParser.parseFixMsgBytes()

        then:
        fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        assertSimpleNewOrderSingle(fixMessage)
        fixMessage.getStartIndex() == 0
        fixMessage.getEndIndex() == simpleNewOrderSingle.length() + "garbage garbage".length() - 1
    }

    def "should parse execution report message, message with repeating group(382)"() {
        setup:
        String message = "8=FIX.4.2\u00019=378\u000135=8\u0001128=XYZ\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20090323-15:40:35\u000155=CVS\u000137=NF 0542/03232009\u000111=NF 0542/03232009\u000117=NF 0542/03232009001001001" +
                         "\u000120=0\u000139=2\u0001150=2\u000154=1\u000138=100\u000140=1\u000159=0\u000131=25.4800\u000132=100\u000114=0\u00016=0\u0001151=0\u000160=20090323-15:40:30\u000158=Fill\u000130=N\u000176=0034\u0001207=N\u0001" +
                         "47=A\u0001382=1\u0001375=TOD\u0001337=0000\u0001437=100\u0001438=20090330-23:40:35\u000129=1\u000163=0\u000110=080\u0001"
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))
        fixMessageParser.setFixMessage(fixMessage)

        when:
        fixMessageParser.parseFixMsgBytes()

        then:
        fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        fixMessage.getField(8).charSequenceValue.toString() == "FIX.4.2"
        fixMessage.getField(9).longValue == 378
        fixMessage.getField(35).charSequenceValue.toString() == "8"
        fixMessage.getField(34).longValue == 5
        fixMessage.getField(49).charSequenceValue.toString() == "CCG"
        fixMessage.getField(56).charSequenceValue.toString() == "ABC_DEFG01"
        fixMessage.getField(52).timestampValue == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20090323-15:40:35", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getField(55).charSequenceValue.toString() == "CVS"
        fixMessage.getField(37).charSequenceValue.toString() == "NF 0542/03232009"
        fixMessage.getField(11).charSequenceValue.toString() == "NF 0542/03232009"
        fixMessage.getField(17).charSequenceValue.toString() == "NF 0542/03232009001001001"
        fixMessage.getField(39).charValue == "2" as char
        fixMessage.getField(150).charValue == "2" as char
        fixMessage.getField(54).charValue == "1" as char
        fixMessage.getField(38).doubleUnscaledValue == 100
        fixMessage.getField(38).scale == 0
        fixMessage.getField(40).charValue == "1" as char
        fixMessage.getField(59).charValue == "0" as char
        fixMessage.getField(31).doubleUnscaledValue == 254800
        fixMessage.getField(31).scale == 4
        fixMessage.getField(32).doubleUnscaledValue == 100
        fixMessage.getField(32).scale == 0
        fixMessage.getField(14).doubleUnscaledValue == 0
        fixMessage.getField(14).scale == 0
        fixMessage.getField(6).doubleUnscaledValue == 0
        fixMessage.getField(6).scale == 0
        fixMessage.getField(151).doubleUnscaledValue == 0
        fixMessage.getField(151).scale == 0
        fixMessage.getField(60).timestampValue == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20090323-15:40:30", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getField(58).charSequenceValue.toString() == "Fill"
        fixMessage.getField(30).charSequenceValue.toString() == "N"
        fixMessage.getField(207).charSequenceValue.toString() == "N"
        def repeatingGroupField = fixMessage.getField(382)
        repeatingGroupField.longValue == 1
        repeatingGroupField.@fieldValue.valueTypeSet == FieldType.GROUP
        repeatingGroupField.@fieldValue.repetitionCounter == 1
        repeatingGroupField.getFieldForGivenRepetition(0, 375).charSequenceValue.toString() == "TOD"
        repeatingGroupField.getFieldForGivenRepetition(0, 337).charSequenceValue.toString() == "0000"
        repeatingGroupField.getFieldForGivenRepetition(0, 437).doubleUnscaledValue == 100
        repeatingGroupField.getFieldForGivenRepetition(0, 437).scale == 0
        repeatingGroupField.getFieldForGivenRepetition(0, 438).timestampValue == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20090330-23:40:35", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getField(29).charValue == "1" as char
        fixMessage.getField(63).charSequenceValue.toString() == "0"
        fixMessage.getField(10).charSequenceValue.toString() == "080"
        fixMessage.getStartIndex() == 0
        fixMessage.getEndIndex() == message.length() - 1
    }

    def "should parse execution report message, message with multiple occurrences of repeating group(382)"() {
        setup:
        String message = "8=FIX.4.2\u00019=378\u000135=8\u0001128=XYZ\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20090323-15:40:35\u000155=CVS\u000137=NF 0542/03232009\u000111=NF 0542/03232009\u000117=NF 0542/03232009001001001" +
                         "\u000120=0\u000139=2\u0001150=2\u000154=1\u000138=100\u000140=1\u000159=0\u000131=25.4800\u000132=100\u000114=0\u00016=0\u0001151=0\u000160=20090323-15:40:30\u000158=Fill\u000130=N\u000176=0034\u0001207=N\u0001" +
                         "47=A\u0001382=2\u0001375=TOD\u0001337=0000\u0001437=100\u0001438=20090330-23:40:35\u0001375=TOD2\u0001337=0001\u0001437=101\u0001438=20090330-23:40:36\u000129=1\u000163=0\u000110=080\u0001"
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))
        fixMessageParser.setFixMessage(fixMessage)

        when:
        fixMessageParser.parseFixMsgBytes()

        then:
        fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        fixMessage.getField(8).charSequenceValue.toString() == "FIX.4.2"
        fixMessage.getField(9).longValue == 378
        fixMessage.getField(35).charSequenceValue.toString() == "8"
        fixMessage.getField(34).longValue == 5
        fixMessage.getField(49).charSequenceValue.toString() == "CCG"
        fixMessage.getField(56).charSequenceValue.toString() == "ABC_DEFG01"
        fixMessage.getField(52).timestampValue == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20090323-15:40:35", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getField(55).charSequenceValue.toString() == "CVS"
        fixMessage.getField(37).charSequenceValue.toString() == "NF 0542/03232009"
        fixMessage.getField(11).charSequenceValue.toString() == "NF 0542/03232009"
        fixMessage.getField(17).charSequenceValue.toString() == "NF 0542/03232009001001001"
        fixMessage.getField(39).charValue == "2" as char
        fixMessage.getField(150).charValue == "2" as char
        fixMessage.getField(54).charValue == "1" as char
        fixMessage.getField(38).doubleUnscaledValue == 100
        fixMessage.getField(38).scale == 0
        fixMessage.getField(40).charValue == "1" as char
        fixMessage.getField(59).charValue == "0" as char
        fixMessage.getField(31).doubleUnscaledValue == 254800
        fixMessage.getField(31).scale == 4
        fixMessage.getField(32).doubleUnscaledValue == 100
        fixMessage.getField(32).scale == 0
        fixMessage.getField(14).doubleUnscaledValue == 0
        fixMessage.getField(14).scale == 0
        fixMessage.getField(6).doubleUnscaledValue == 0
        fixMessage.getField(6).scale == 0
        fixMessage.getField(151).doubleUnscaledValue == 0
        fixMessage.getField(151).scale == 0
        fixMessage.getField(60).timestampValue == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20090323-15:40:30", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getField(58).charSequenceValue.toString() == "Fill"
        fixMessage.getField(30).charSequenceValue.toString() == "N"
        fixMessage.getField(207).charSequenceValue.toString() == "N"
        def repeatingGroupField = fixMessage.getField(382)
        repeatingGroupField.longValue == 2
        repeatingGroupField.@fieldValue.valueTypeSet == FieldType.GROUP
        repeatingGroupField.@fieldValue.repetitionCounter == 2
        repeatingGroupField.getFieldForGivenRepetition(0,375).charSequenceValue.toString() == "TOD"
        repeatingGroupField.getFieldForGivenRepetition(0,337).charSequenceValue.toString() == "0000"
        repeatingGroupField.getFieldForGivenRepetition(0,437).doubleUnscaledValue == 100
        repeatingGroupField.getFieldForGivenRepetition(0,437).scale == 0
        repeatingGroupField.getFieldForGivenRepetition(0,438).timestampValue == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20090330-23:40:35", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        repeatingGroupField.getFieldForGivenRepetition(1,375).charSequenceValue.toString() == "TOD2"
        repeatingGroupField.getFieldForGivenRepetition(1,337).charSequenceValue.toString() == "0001"
        repeatingGroupField.getFieldForGivenRepetition(1,437).doubleUnscaledValue == 101
        repeatingGroupField.getFieldForGivenRepetition(1,437).scale == 0
        repeatingGroupField.getFieldForGivenRepetition(1,438).timestampValue == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20090330-23:40:36", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        fixMessage.getField(29).charValue == "1" as char
        fixMessage.getField(63).charSequenceValue.toString() == "0"
        fixMessage.getField(10).charSequenceValue.toString() == "080"
        fixMessage.getStartIndex() == 0
        fixMessage.getEndIndex() == message.length() - 1
    }

    def "should parse fake confirmation message, message with nested repeating groups"() {
        setup:
        String message = "8=FIX.4.4\u00019=378\u000135=AK\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20090323-15:40:35\u000185=2\u0001787=C\u0001781=2\u0001782=ID1\u0001782=ID2\u0001787=D\u0001781=2\u0001782=ID3\u0001782=ID4" +
                         "\u000110=080\u0001"
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))
        fixMessageParser.setFixMessage(fixMessage)

        when:
        fixMessageParser.parseFixMsgBytes()

        then:
        fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        fixMessage.getField(8).charSequenceValue.toString() == "FIX.4.4"
        fixMessage.getField(9).longValue == 378
        fixMessage.getField(35).charSequenceValue.toString() == "AK"
        fixMessage.getField(34).longValue == 5
        fixMessage.getField(49).charSequenceValue.toString() == "CCG"
        fixMessage.getField(56).charSequenceValue.toString() == "ABC_DEFG01"
        fixMessage.getField(52).timestampValue == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20090323-15:40:35", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        def dlvyInstGroup = fixMessage.getField(85)
        dlvyInstGroup.longValue == 2
        dlvyInstGroup.@fieldValue.valueTypeSet == FieldType.GROUP
        dlvyInstGroup.@fieldValue.repetitionCounter == 2
        dlvyInstGroup.getFieldForGivenRepetition(0,787).charValue == 'C' as char
        def settlPartiesGroup1 = dlvyInstGroup.getFieldForGivenRepetition(0,781)
        settlPartiesGroup1.longValue == 2
        settlPartiesGroup1.@fieldValue.valueTypeSet == FieldType.GROUP
        settlPartiesGroup1.@fieldValue.repetitionCounter == 2
        settlPartiesGroup1.getFieldForGivenRepetition(0,782).charSequenceValue.toString() == 'ID1'
        settlPartiesGroup1.getFieldForGivenRepetition(1,782).charSequenceValue.toString() == 'ID2'
        dlvyInstGroup.getFieldForGivenRepetition(1,787).charValue == 'D' as char
        def settlPartiesGroup2 = dlvyInstGroup.getFieldForGivenRepetition(1,781)
        settlPartiesGroup2.longValue == 2
        settlPartiesGroup2.@fieldValue.valueTypeSet == FieldType.GROUP
        settlPartiesGroup2.@fieldValue.repetitionCounter == 2
        settlPartiesGroup2.getFieldForGivenRepetition(0,782).charSequenceValue.toString() == 'ID3'
        settlPartiesGroup2.getFieldForGivenRepetition(1,782).charSequenceValue.toString() == 'ID4'
        fixMessage.getStartIndex() == 0
        fixMessage.getEndIndex() == message.length() - 1
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
        String message = "8=FIX.4.4\u00019=378\u000135=AK\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20090323-15:40:"
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))
        fixMessageParser.setFixMessage(fixMessage)

        when:
        fixMessageParser.parseFixMsgBytes()

        then:
        !fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == message.length() - 1
        fixMessage.getField(8).charSequenceValue.toString() == "FIX.4.4"
        fixMessage.getField(9).longValue == 378
        fixMessage.getField(35).charSequenceValue.toString() == "AK"
        fixMessage.getField(34).longValue == 5
        fixMessage.getField(49).charSequenceValue.toString() == "CCG"
        fixMessage.getField(56).charSequenceValue.toString() == "ABC_DEFG01"
        !fixMessage.getField(52).isValueSet()
        !fixMessageParser.canContinueParsing()
        fixMessage.getStartIndex() == 0
        fixMessage.getEndIndex() == FixMessage.NOT_SET
    }

    def "should parse unfinished message that ends with SOH"() {
        setup:
        String message = "8=FIX.4.4\u00019=378\u000135=AK\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20090323-15:40:35\u0001"
        byteBufComposer.addByteBuf(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))
        fixMessageParser.setFixMessage(fixMessage)

        when:
        fixMessageParser.parseFixMsgBytes()

        then:
        !fixMessageParser.isDone()
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == message.length() - 1
        fixMessage.getField(8).charSequenceValue.toString() == "FIX.4.4"
        fixMessage.getField(9).longValue == 378
        fixMessage.getField(35).charSequenceValue.toString() == "AK"
        fixMessage.getField(34).longValue == 5
        fixMessage.getField(49).charSequenceValue.toString() == "CCG"
        fixMessage.getField(56).charSequenceValue.toString() == "ABC_DEFG01"
        fixMessage.getField(52).timestampValue == Instant.parse("2009-03-23T15:40:35Z").toEpochMilli()
        !fixMessageParser.canContinueParsing()
        fixMessage.getStartIndex() == 0
        fixMessage.getEndIndex() == FixMessage.NOT_SET
    }

    def "should set new fix message"() {
        setup:
        def byteBufComposer = new ByteBufComposer(1)
        byteBufComposer.readerIndex(10)
        fixMessageParser = new FixMessageParser(byteBufComposer, fixSpec50SP2)
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage = 666

        when:
        fixMessageParser.setFixMessage(fixMessage)

        then:
        fixMessageParser.storedEndIndexOfLastUnfinishedMessage == 0
        fixMessage.messageByteSource == byteBufComposer
        fixMessage.getStartIndex() == byteBufComposer.readerIndex()
    }

    def "should parse simple new order single fragmented with garbage"() {
        setup:
        def packet1 = Unpooled.wrappedBuffer(simpleNewOrderSingleWithGarbage.substring(0, packet1End).getBytes(StandardCharsets.US_ASCII))
        byteBufComposer.addByteBuf(packet1)
        fixMessageParser.setFixMessage(fixMessage)
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
        def byteBufComposer = new ByteBufComposer(1)
        fixMessageParser = new FixMessageParser(byteBufComposer, fixSpec50SP2)
        fixMessage.retain()
        fixMessageParser.@fixMessage = fixMessage
        fixMessageParser.@storedEndIndexOfLastUnfinishedMessage = 666
        fixMessageParser.@parsingRepeatingGroup = true
        fixMessageParser.@groupFieldsStack.add(new Field(TestSpec.USABLE_CHILD_PAIR_SPEC_FIELD_NUMBER, new FieldCodec()))

        when:
        fixMessageParser.reset()

        then:
        fixMessageParser.@bytesToParse.is(byteBufComposer)
        fixMessageParser.@fixMessage == null
        fixMessageParser.@storedEndIndexOfLastUnfinishedMessage == 0
        !fixMessageParser.@parsingRepeatingGroup
        fixMessageParser.@groupFieldsStack.isEmpty()
        fixMessage.refCnt() == 0
    }

    private final static String simpleNewOrderSingle = "8=FIX.4.2\u00019=145\u000135=D\u000134=4\u000149=ABC_DEFG01\u000152=20090323-15:40:29\u000156=CCG\u0001115=XYZ\u000111=NF " +
                                                       "0542/03232009\u000154=1\u000138=100\u000155=CVS\u000140=1\u000159=0\u000147=A\u000160=20090323-15:40:29\u000121=1\u0001207=N\u000110=139\u0001"

    private final static String simpleNewOrderSingleWithGarbage = "garbae8=FIX.4.2\u00019=145\u000135=D\u000134=4\u000149=ABC_DEFG01\u000152=20090323-15:40:29\u000156=CCG\u0001115=XYZ\u000111=NF " +
                                                                  "0542/03232009\u000154=1\u000138=100\u000155=CVS\u000140=1\u000159=0\u000147=A\u000160=20090323-15:40:29\u000121=1\u0001207=N\u000110=139\u0001garbage"

    private static void assertSimpleNewOrderSingle(FixMessage fixMessage) {
        assert fixMessage.getField(8).charSequenceValue.toString() == "FIX.4.2"
        assert fixMessage.getField(9).longValue == 145
        assert fixMessage.getField(35).charSequenceValue.toString() == "D"
        assert fixMessage.getField(34).longValue == 4
        assert fixMessage.getField(49).charSequenceValue.toString() == "ABC_DEFG01"
        assert fixMessage.getField(52).timestampValue == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20090323-15:40:29", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        assert fixMessage.getField(56).charSequenceValue.toString() == "CCG"
        assert fixMessage.getField(115).charSequenceValue.toString() == "XYZ"
        assert fixMessage.getField(11).charSequenceValue.toString() == "NF 0542/03232009"
        assert fixMessage.getField(54).charValue == "1" as char
        assert fixMessage.getField(38).doubleUnscaledValue == 100
        assert fixMessage.getField(38).scale == 0
        assert fixMessage.getField(55).charSequenceValue.toString() == "CVS"
        assert fixMessage.getField(40).charValue == "1" as char
        assert fixMessage.getField(59).charValue == "0" as char
        assert fixMessage.getField(60).timestampValue == FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse("20090323-15:40:29", { LocalDateTime.from(it) }).toInstant(ZoneOffset.UTC).toEpochMilli()
        assert fixMessage.getField(21).charValue == "1" as char
        assert fixMessage.getField(207).charSequenceValue.toString() == "N"
        assert fixMessage.getField(10).charSequenceValue.toString() == "139"
    }
}
