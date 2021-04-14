package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.parser.FixSpec50SP2
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Instant

class FixMessageToStringTest extends Specification {

    private static final FixSpec50SP2 SPEC = new FixSpec50SP2()

    @AutoCleanup
    private NotPoolableFixMessage fixMessage = new NotPoolableFixMessage()

    def "should toString short message"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, "FIXT.1.1")
        fixMessage.setLongValue(FixConstants.BODY_LENGTH_FIELD_NUMBER, 10)
        fixMessage.setBooleanValue(FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER, true)
        fixMessage.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, Instant.parse("2021-03-29T18:51:13Z").toEpochMilli())
        fixMessage.setDoubleValue(151, 123, 2 as short)
        fixMessage.setCharValue(150, '0' as char)
        fixMessage.setCharSequenceValue(448, 453, 0 as byte, 0 as byte, "id1")
        fixMessage.setCharSequenceValue(448, 453, 1 as byte, 0 as byte, "id2")
        fixMessage.retain()

        when:
        def result = FixMessageToString.toString(fixMessage, wholeMessage, SPEC)

        then:
        result == "FixMessage -> 8=FIXT.1.1|9=10|43=Y|52=1617043873000|150=0|151=1.23|453=2, refCnt=2" //I know, no repeating group content :/

        where:
        wholeMessage << [true, false]
    }

    def "should shorten long message"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, "FIXT.1.1")
        fixMessage.setLongValue(FixConstants.BODY_LENGTH_FIELD_NUMBER, 10)
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 11)
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, "senderCompId")
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, "targetCompId")
        fixMessage.setBooleanValue(FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER, true)
        fixMessage.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, Instant.parse("2021-03-29T18:51:13Z").toEpochMilli())
        fixMessage.setCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER, "some text")
        fixMessage.setCharSequenceValue(55, "symbol")
        fixMessage.setDoubleValue(151, 123, 2 as short)
        fixMessage.setCharValue(150, '0' as char)
        fixMessage.setCharSequenceValue(FixConstants.CHECK_SUM_FIELD_NUMBER, "001")

        when:
        def result = FixMessageToString.toString(fixMessage, false, SPEC)

        then:
        result == "FixMessage -> 8=FIXT.1.1|9=10|49=senderCompId|56=targetCompId|34=11|43=Y|52=1617043873000|10=001|58=some text|55=symbol..., refCnt=1"
    }

    def "should not shorten long message"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, "FIXT.1.1")
        fixMessage.setLongValue(FixConstants.BODY_LENGTH_FIELD_NUMBER, 10)
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 11)
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, "senderCompId")
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, "targetCompId")
        fixMessage.setBooleanValue(FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER, true)
        fixMessage.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, Instant.parse("2021-03-29T18:51:13Z").toEpochMilli())
        fixMessage.setCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER, "some text")
        fixMessage.setCharSequenceValue(55, "symbol")
        fixMessage.setDoubleValue(151, 123, 2 as short)
        fixMessage.setCharValue(150, '0' as char)
        fixMessage.setCharSequenceValue(FixConstants.CHECK_SUM_FIELD_NUMBER, "001")

        when:
        def result = FixMessageToString.toString(fixMessage, true, SPEC)

        then:
        result == "FixMessage -> 8=FIXT.1.1|9=10|49=senderCompId|56=targetCompId|34=11|43=Y|52=1617043873000|10=001|58=some text|55=symbol|150=0|151=1.23, refCnt=1"
    }
}
