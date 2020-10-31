package io.github.zlooo.fixyou.fix.commons.session

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.SessionID
import org.assertj.core.api.Assertions
import spock.lang.Specification

class SessionIDUtilsTest extends Specification {

    private static SessionID sessionID = new SessionID('FIXT.1.1'.toCharArray(), 8, 'senderCompID'.toCharArray(), 12, 'targetCompID'.toCharArray(), 12)

    def "should set all session id fields"() {
        setup:
        def fixMessage = new FixMessage(new FieldCodec())

        when:
        SessionIDUtils.setSessionIdFields(fixMessage, sessionID)

        then:
        def sessionIdFieldsMap = sessionIDFieldsMap()
        for (Map.Entry<SessionID.Fields, Integer> mappingEntry : sessionIdFieldsMap) {
            assert fixMessage.getField(mappingEntry.value).charSequenceValue.chars == sessionID.properties[mappingEntry.key.name()]
        }
    }

    def "should build session id from fix message"() {
        setup:
        def fixMessage = new FixMessage(new FieldCodec())
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).charSequenceValue = sessionID.beginString
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).charSequenceValue = sessionID.senderCompID
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).charSequenceValue = sessionID.targetCompID

        expect:
        SessionIDUtils.buildSessionID(fixMessage, false) == sessionID
    }

    def "should build session id from fix message with comp ids flipped"() {
        setup:
        def fixMessage = new FixMessage(new FieldCodec())
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).charSequenceValue = sessionID.beginString
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).charSequenceValue = sessionID.senderCompID
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).charSequenceValue = sessionID.targetCompID

        expect:
        SessionIDUtils.buildSessionID(fixMessage, true) == new SessionID(sessionID.beginString, sessionID.beginString.length, sessionID.targetCompID, sessionID.targetCompID.length, sessionID.senderCompID, sessionID.senderCompID.length)
    }

    def "should check if comp ids are equal"() {
        setup:
        def fixMessage = new FixMessage(new FieldCodec())
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).charSequenceValue = senderCompID
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).charSequenceValue = targetCompID

        expect:
        SessionIDUtils.checkCompIDs(fixMessage, sessionIdToCompare, flipIDs) == result

        where:
        senderCompID | targetCompID | flipIDs | sessionIdToCompare                                       | result
        ca('scid')   | ca('tcid')   | false   | new SessionID(ca('bs'), 2, ca('scid'), 4, ca('tcid'), 4) | true
        ca('scid1')  | ca('tcid')   | false   | new SessionID(ca('bs'), 2, ca('scid'), 4, ca('tcid'), 4) | false
        ca('scid')   | ca('tcid1')  | false   | new SessionID(ca('bs'), 2, ca('scid'), 4, ca('tcid'), 4) | false
        ca('tcid')   | ca('scid')   | true    | new SessionID(ca('bs'), 2, ca('scid'), 4, ca('tcid'), 4) | true
        ca('tcid1')  | ca('scid')   | true    | new SessionID(ca('bs'), 2, ca('scid'), 4, ca('tcid'), 4) | false
        ca('tcid')   | ca('scid1')  | true    | new SessionID(ca('bs'), 2, ca('scid'), 4, ca('tcid'), 4) | false
    }

    Map<SessionID.Fields, Integer> sessionIDFieldsMap() {
        def result = [(SessionID.Fields.beginString) : FixConstants.BEGIN_STRING_FIELD_NUMBER,
                      (SessionID.Fields.senderCompID): FixConstants.SENDER_COMP_ID_FIELD_NUMBER,
                      (SessionID.Fields.targetCompID): FixConstants.TARGET_COMP_ID_FIELD_NUMBER]
        Assertions.assertThat(result).doesNotContainValue(null).containsOnlyKeys(SessionID.Fields.values())
        return result
    }

    private static char[] ca(String stringToChange) {
        stringToChange.toCharArray()
    }
}
