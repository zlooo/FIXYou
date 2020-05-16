package pl.zlooo.fixyou.fix.commons.session

import org.assertj.core.api.Assertions
import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.fix.commons.TestSpec
import pl.zlooo.fixyou.parser.model.FixMessage
import pl.zlooo.fixyou.session.SessionID
import spock.lang.Specification

class SessionIDUtilsTest extends Specification {

    private static SessionID sessionID = new SessionID('FIXT.1.1'.toCharArray(), 'senderCompID'.toCharArray(), 'targetCompID'.toCharArray())

    def "should set all session id fields"() {
        setup:
        def fixMessage = new FixMessage(TestSpec.INSTANCE)

        when:
        SessionIDUtils.setSessionIdFields(fixMessage, sessionID)

        then:
        def sessionIdFieldsMap = sessionIDFieldsMap()
        for (Map.Entry<SessionID.Fields, Integer> mappingEntry : sessionIdFieldsMap) {
            assert fixMessage.getField(mappingEntry.value).value == sessionID.properties[mappingEntry.key.name()]
        }
    }

    def "should build session id from fix message"() {
        setup:
        def fixMessage = new FixMessage(TestSpec.INSTANCE)
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).value = sessionID.beginString
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = sessionID.senderCompID
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = sessionID.targetCompID

        expect:
        SessionIDUtils.buildSessionID(fixMessage, false) == sessionID
    }

    def "should build session id from fix message with comp ids flipped"() {
        setup:
        def fixMessage = new FixMessage(TestSpec.INSTANCE)
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).value = sessionID.beginString
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = sessionID.senderCompID
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = sessionID.targetCompID

        expect:
        SessionIDUtils.buildSessionID(fixMessage, true) == new SessionID(sessionID.beginString, sessionID.targetCompID, sessionID.senderCompID)
    }

    def "should check if comp ids are equal"() {
        setup:
        def fixMessage = new FixMessage(TestSpec.INSTANCE)
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = senderCompID
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = targetCompID

        expect:
        SessionIDUtils.checkCompIDs(fixMessage, sessionIdToCompare, flipIDs) == result

        where:
        senderCompID | targetCompID | flipIDs | sessionIdToCompare                              | result
        ca('scid')   | ca('tcid')   | false   | new SessionID(ca('bs'), ca('scid'), ca('tcid')) | true
        ca('scid1')  | ca('tcid')   | false   | new SessionID(ca('bs'), ca('scid'), ca('tcid')) | false
        ca('scid')   | ca('tcid1')  | false   | new SessionID(ca('bs'), ca('scid'), ca('tcid')) | false
        ca('tcid')   | ca('scid')   | true    | new SessionID(ca('bs'), ca('scid'), ca('tcid')) | true
        ca('tcid1')  | ca('scid')   | true    | new SessionID(ca('bs'), ca('scid'), ca('tcid')) | false
        ca('tcid')   | ca('scid1')  | true    | new SessionID(ca('bs'), ca('scid'), ca('tcid')) | false
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
