package io.github.zlooo.fixyou.parser

import io.netty.buffer.Unpooled
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class FixMessageReaderTest extends Specification {

    private static final FixSpec50SP2 fixSpec50SP2 = new FixSpec50SP2()
    @AutoCleanup("close")
    private io.github.zlooo.fixyou.parser.model.FixMessage fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(fixSpec50SP2)
    private FixMessageReader fixMessageReader = new FixMessageReader(fixMessage)

    def "should parse new order single message, simple message case"() {
        setup:
        String message = "8=FIX.4.2\u00019=145\u000135=D\u000134=4\u000149=ABC_DEFG01\u000152=20090323-15:40:29\u000156=CCG\u0001115=XYZ\u000111=NF 0542/03232009\u000154=1\u000138=100\u000155=CVS\u000140=1\u000159=0\u000147=A\u0001" +
                         "60=20090323-15:40:29\u000121=1\u0001207=N\u000110=139\u0001"
        fixMessageReader.setFixBytes(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageReader.parseFixMsgBytes()

        then:
        fixMessageReader.isDone()
        fixMessage.getField(8).value == "FIX.4.2".toCharArray()
        fixMessage.getField(9).value == 145
        fixMessage.getField(35).value == "D".toCharArray()
        fixMessage.getField(34).value == 4
        fixMessage.getField(49).value == "ABC_DEFG01".toCharArray()
        fixMessage.getField(52).value == "20090323-15:40:29".toCharArray()
        fixMessage.getField(56).value == "CCG".toCharArray()
        fixMessage.getField(115).value == "XYZ".toCharArray()
        fixMessage.getField(11).value == "NF 0542/03232009".toCharArray()
        fixMessage.getField(54).value == "1" as char
        fixMessage.getField(38).value == 100
        fixMessage.getField(38).scale == 0
        fixMessage.getField(55).value == "CVS".toCharArray()
        fixMessage.getField(40).value == "1" as char
        fixMessage.getField(59).value == "0" as char
        fixMessage.getField(60).value == "20090323-15:40:29".toCharArray()
        fixMessage.getField(21).value == "1" as char
        fixMessage.getField(207).value == "N".toCharArray()
        fixMessage.getField(10).value == "139".toCharArray()
    }

    def "should parse execution report message, message with repeating group(382)"() {
        setup:
        String message = "8=FIX.4.2\u00019=378\u000135=8\u0001128=XYZ\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20090323-15:40:35\u000155=CVS\u000137=NF 0542/03232009\u000111=NF 0542/03232009\u000117=NF 0542/03232009001001001" +
                         "\u000120=0\u000139=2\u0001150=2\u000154=1\u000138=100\u000140=1\u000159=0\u000131=25.4800\u000132=100\u000114=0\u00016=0\u0001151=0\u000160=20090323-15:40:30\u000158=Fill\u000130=N\u000176=0034\u0001207=N\u0001" +
                         "47=A\u0001382=1\u0001375=TOD\u0001337=0000\u0001437=100\u0001438=1243\u000129=1\u000163=0\u000110=080\u0001"
        fixMessageReader.setFixBytes(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageReader.parseFixMsgBytes()

        then:
        fixMessageReader.isDone()
        fixMessage.getField(8).value == "FIX.4.2".toCharArray()
        fixMessage.getField(9).value == 378
        fixMessage.getField(35).value == "8".toCharArray()
        fixMessage.getField(34).value == 5
        fixMessage.getField(49).value == "CCG".toCharArray()
        fixMessage.getField(56).value == "ABC_DEFG01".toCharArray()
        fixMessage.getField(52).value == "20090323-15:40:35".toCharArray()
        fixMessage.getField(55).value == "CVS".toCharArray()
        fixMessage.getField(37).value == "NF 0542/03232009".toCharArray()
        fixMessage.getField(11).value == "NF 0542/03232009".toCharArray()
        fixMessage.getField(17).value == "NF 0542/03232009001001001".toCharArray()
        fixMessage.getField(39).value == "2" as char
        fixMessage.getField(150).value == "2" as char
        fixMessage.getField(54).value == "1" as char
        fixMessage.getField(38).value == 100
        fixMessage.getField(38).scale == 0
        fixMessage.getField(40).value == "1" as char
        fixMessage.getField(59).value == "0" as char
        fixMessage.getField(31).value == 254800
        fixMessage.getField(31).scale == 4
        fixMessage.getField(32).value == 100
        fixMessage.getField(32).scale == 0
        fixMessage.getField(14).value == 0
        fixMessage.getField(14).scale == 0
        fixMessage.getField(6).value == 0
        fixMessage.getField(6).scale == 0
        fixMessage.getField(151).value == 0
        fixMessage.getField(151).scale == 0
        fixMessage.getField(60).value == "20090323-15:40:30".toCharArray()
        fixMessage.getField(58).value == "Fill".toCharArray()
        fixMessage.getField(30).value == "N".toCharArray()
        fixMessage.getField(207).value == "N".toCharArray()
        def repeatingGroupField = fixMessage.getField(382)
        repeatingGroupField instanceof io.github.zlooo.fixyou.parser.model.GroupField
        repeatingGroupField.@numberOfRepetitionsRead == 1
        repeatingGroupField.@numberOfRepetitions == 0
        repeatingGroupField.getField(0, 375).value == "TOD".toCharArray()
        repeatingGroupField.getField(0, 337).value == "0000".toCharArray()
        repeatingGroupField.getField(0, 437).value == 100
        repeatingGroupField.getField(0, 437).scale == 0
        repeatingGroupField.getField(0, 438).value == "1243".toCharArray()
        fixMessage.getField(29).value == "1" as char
        fixMessage.getField(63).value == "0".toCharArray()
        fixMessage.getField(10).value == "080".toCharArray()
    }

    def "should parse execution report message, message with multiple occurrences of repeating group(382)"() {
        setup:
        String message = "8=FIX.4.2\u00019=378\u000135=8\u0001128=XYZ\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20090323-15:40:35\u000155=CVS\u000137=NF 0542/03232009\u000111=NF 0542/03232009\u000117=NF 0542/03232009001001001" +
                         "\u000120=0\u000139=2\u0001150=2\u000154=1\u000138=100\u000140=1\u000159=0\u000131=25.4800\u000132=100\u000114=0\u00016=0\u0001151=0\u000160=20090323-15:40:30\u000158=Fill\u000130=N\u000176=0034\u0001207=N\u0001" +
                         "47=A\u0001382=2\u0001375=TOD\u0001337=0000\u0001437=100\u0001438=1243\u0001375=TOD2\u0001337=0001\u0001437=101\u0001438=1244\u000129=1\u000163=0\u000110=080\u0001"
        fixMessageReader.setFixBytes(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageReader.parseFixMsgBytes()

        then:
        fixMessageReader.isDone()
        fixMessage.getField(8).value == "FIX.4.2".toCharArray()
        fixMessage.getField(9).value == 378
        fixMessage.getField(35).value == "8".toCharArray()
        fixMessage.getField(34).value == 5
        fixMessage.getField(49).value == "CCG".toCharArray()
        fixMessage.getField(56).value == "ABC_DEFG01".toCharArray()
        fixMessage.getField(52).value == "20090323-15:40:35".toCharArray()
        fixMessage.getField(55).value == "CVS".toCharArray()
        fixMessage.getField(37).value == "NF 0542/03232009".toCharArray()
        fixMessage.getField(11).value == "NF 0542/03232009".toCharArray()
        fixMessage.getField(17).value == "NF 0542/03232009001001001".toCharArray()
        fixMessage.getField(39).value == "2" as char
        fixMessage.getField(150).value == "2" as char
        fixMessage.getField(54).value == "1" as char
        fixMessage.getField(38).value == 100
        fixMessage.getField(38).scale == 0
        fixMessage.getField(40).value == "1" as char
        fixMessage.getField(59).value == "0" as char
        fixMessage.getField(31).value == 254800
        fixMessage.getField(31).scale == 4
        fixMessage.getField(32).value == 100
        fixMessage.getField(32).scale == 0
        fixMessage.getField(14).value == 0
        fixMessage.getField(14).scale == 0
        fixMessage.getField(6).value == 0
        fixMessage.getField(6).scale == 0
        fixMessage.getField(151).value == 0
        fixMessage.getField(151).scale == 0
        fixMessage.getField(60).value == "20090323-15:40:30".toCharArray()
        fixMessage.getField(58).value == "Fill".toCharArray()
        fixMessage.getField(30).value == "N".toCharArray()
        fixMessage.getField(207).value == "N".toCharArray()
        def repeatingGroupField = fixMessage.getField(382)
        repeatingGroupField instanceof io.github.zlooo.fixyou.parser.model.GroupField
        repeatingGroupField.@numberOfRepetitionsRead == 2
        repeatingGroupField.@numberOfRepetitions == 1
        repeatingGroupField.getField(0, 375).value == "TOD".toCharArray()
        repeatingGroupField.getField(0, 337).value == "0000".toCharArray()
        repeatingGroupField.getField(0, 437).value == 100
        repeatingGroupField.getField(0, 437).scale == 0
        repeatingGroupField.getField(0, 438).value == "1243".toCharArray()
        repeatingGroupField.getField(1, 375).value == "TOD2".toCharArray()
        repeatingGroupField.getField(1, 337).value == "0001".toCharArray()
        repeatingGroupField.getField(1, 437).value == 101
        repeatingGroupField.getField(1, 437).scale == 0
        repeatingGroupField.getField(1, 438).value == "1244".toCharArray()
        fixMessage.getField(29).value == "1" as char
        fixMessage.getField(63).value == "0".toCharArray()
        fixMessage.getField(10).value == "080".toCharArray()
    }

    def "should parse fake confirmation message, message with nested repeating groups"() {
        setup:
        String message = "8=FIX.4.4\u00019=378\u000135=AK\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20090323-15:40:35\u000185=2\u0001787=C\u0001781=2\u0001782=ID1\u0001782=ID2\u0001787=C\u0001781=2\u0001782=ID3\u0001782=ID4" +
                         "\u000110=080\u0001"
        fixMessageReader.setFixBytes(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageReader.parseFixMsgBytes()

        then:
        fixMessageReader.isDone()
        fixMessage.getField(8).value == "FIX.4.4".toCharArray()
        fixMessage.getField(9).value == 378
        fixMessage.getField(35).value == "AK".toCharArray()
        fixMessage.getField(34).value == 5
        fixMessage.getField(49).value == "CCG".toCharArray()
        fixMessage.getField(56).value == "ABC_DEFG01".toCharArray()
        fixMessage.getField(52).value == "20090323-15:40:35".toCharArray()
        def dlvyInstGroup = fixMessage.getField(85)
        dlvyInstGroup instanceof io.github.zlooo.fixyou.parser.model.GroupField
        dlvyInstGroup.@numberOfRepetitionsRead == 2
        dlvyInstGroup.@numberOfRepetitions == 1
        dlvyInstGroup.getField(0, 787).value == 'C' as char
        def settlPartiesGroup1 = dlvyInstGroup.getField(0, 781)
        settlPartiesGroup1 instanceof io.github.zlooo.fixyou.parser.model.GroupField
        settlPartiesGroup1.@numberOfRepetitionsRead == 2
        settlPartiesGroup1.@numberOfRepetitions == 1
        settlPartiesGroup1.getField(0, 782).value == 'ID1'.toCharArray()
        settlPartiesGroup1.getField(1, 782).value == 'ID2'.toCharArray()
        dlvyInstGroup.getField(1, 787).value == 'C' as char
        def settlPartiesGroup2 = dlvyInstGroup.getField(1, 781)
        settlPartiesGroup2 instanceof io.github.zlooo.fixyou.parser.model.GroupField
        settlPartiesGroup2.@numberOfRepetitionsRead == 2
        settlPartiesGroup2.@numberOfRepetitions == 1
        settlPartiesGroup2.getField(0, 782).value == 'ID3'.toCharArray()
        settlPartiesGroup2.getField(1, 782).value == 'ID4'.toCharArray()
    }

    def "should append fix message bytes when some are left over from previous parsing"() {
        setup:
        fixMessageReader.setFixBytes(Unpooled.wrappedBuffer("part1".getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageReader.setFixBytes(Unpooled.wrappedBuffer("|part2".getBytes(StandardCharsets.US_ASCII)))

        then:
        fixMessageReader.@parseableBytes.toString(StandardCharsets.US_ASCII) == "part1|part2"
    }

    def "should check if underlying buffer is readable"() {
        setup:
        fixMessageReader.setFixBytes(Unpooled.wrappedBuffer("message".getBytes(StandardCharsets.US_ASCII)))
        fixMessageReader.@parseableBytes.readerIndex(readerIndex)
        fixMessageReader.@parseableBytes.writerIndex(writerIndex)

        expect:
        fixMessageReader.isUnderlyingBufferReadable() == expectedResult

        where:
        readerIndex | writerIndex | expectedResult
        0           | 1           | true
        1           | 1           | false
    }

    def "should parse unfinished message"() {
        setup:
        String message = "8=FIX.4.4\u00019=378\u000135=AK\u000134=5\u000149=CCG\u000156=ABC_DEFG01\u000152=20090323-15:40:35"
        fixMessageReader.setFixBytes(Unpooled.wrappedBuffer(message.getBytes(StandardCharsets.US_ASCII)))

        when:
        fixMessageReader.parseFixMsgBytes()

        then:
        !fixMessageReader.isDone()
        fixMessage.getField(8).value == "FIX.4.4".toCharArray()
        fixMessage.getField(9).value == 378
        fixMessage.getField(35).value == "AK".toCharArray()
        fixMessage.getField(34).value == 5
        fixMessage.getField(49).value == "CCG".toCharArray()
        fixMessage.getField(56).value == "ABC_DEFG01".toCharArray()
        !fixMessage.getField(52).isValueSet()
        !fixMessageReader.@parseable
    }
}
