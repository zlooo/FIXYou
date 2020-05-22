package io.github.zlooo.fixyou.fix.commons.session

import io.github.zlooo.fixyou.fix.commons.TestSpec
import org.assertj.core.api.Assertions
import spock.lang.Specification

class SessionIDUtilsTest extends Specification {

    private static io.github.zlooo.fixyou.session.SessionID sessionID = new io.github.zlooo.fixyou.session.SessionID('FIXT.1.1'.toCharArray(), 'senderCompID'.toCharArray(), 'targetCompID'.toCharArray())

    def "should set all session id fields"() {
        setup:
        def fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(TestSpec.INSTANCE)

        when:
        SessionIDUtils.setSessionIdFields(fixMessage, sessionID)

        then:
        def sessionIdFieldsMap = sessionIDFieldsMap()
        for (Map.Entry<io.github.zlooo.fixyou.session.SessionID.Fields, Integer> mappingEntry : sessionIdFieldsMap) {
            assert fixMessage.getField(mappingEntry.value).value == sessionID.properties[mappingEntry.key.name()]
        }
    }

    def "should build session id from fix message"() {
        setup:
        def fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(TestSpec.INSTANCE)
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.BEGIN_STRING_FIELD_NUMBER).value = sessionID.beginString
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = sessionID.senderCompID
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = sessionID.targetCompID

        expect:
        SessionIDUtils.buildSessionID(fixMessage, false) == sessionID
    }

    def "should build session id from fix message with comp ids flipped"() {
        setup:
        def fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(TestSpec.INSTANCE)
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.BEGIN_STRING_FIELD_NUMBER).value = sessionID.beginString
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = sessionID.senderCompID
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = sessionID.targetCompID

        expect:
        SessionIDUtils.buildSessionID(fixMessage, true) == new io.github.zlooo.fixyou.session.SessionID(sessionID.beginString, sessionID.targetCompID, sessionID.senderCompID)
    }

    def "should check if comp ids are equal"() {
        setup:
        def fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(TestSpec.INSTANCE)
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value = senderCompID
        fixMessage.getField(io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value = targetCompID

        expect:
        SessionIDUtils.checkCompIDs(fixMessage, sessionIdToCompare, flipIDs) == result

        where:
        senderCompID | targetCompID | flipIDs | sessionIdToCompare                              | result
        ca('scid')   | ca('tcid')   | false   | new io.github.zlooo.fixyou.session.SessionID(ca('bs'), ca('scid'), ca('tcid')) | true
        ca('scid1')  | ca('tcid')   | false   | new io.github.zlooo.fixyou.session.SessionID(ca('bs'), ca('scid'), ca('tcid')) | false
        ca('scid')   | ca('tcid1')  | false   | new io.github.zlooo.fixyou.session.SessionID(ca('bs'), ca('scid'), ca('tcid')) | false
        ca('tcid')   | ca('scid')   | true    | new io.github.zlooo.fixyou.session.SessionID(ca('bs'), ca('scid'), ca('tcid')) | true
        ca('tcid1')  | ca('scid')   | true    | new io.github.zlooo.fixyou.session.SessionID(ca('bs'), ca('scid'), ca('tcid')) | false
        ca('tcid')   | ca('scid1')  | true    | new io.github.zlooo.fixyou.session.SessionID(ca('bs'), ca('scid'), ca('tcid')) | false
    }

    Map<io.github.zlooo.fixyou.session.SessionID.Fields, Integer> sessionIDFieldsMap() {
        def result = [(io.github.zlooo.fixyou.session.SessionID.Fields.beginString) : io.github.zlooo.fixyou.FixConstants.BEGIN_STRING_FIELD_NUMBER,
                      (io.github.zlooo.fixyou.session.SessionID.Fields.senderCompID): io.github.zlooo.fixyou.FixConstants.SENDER_COMP_ID_FIELD_NUMBER,
                      (io.github.zlooo.fixyou.session.SessionID.Fields.targetCompID): io.github.zlooo.fixyou.FixConstants.TARGET_COMP_ID_FIELD_NUMBER]
        Assertions.assertThat(result).doesNotContainValue(null).containsOnlyKeys(io.github.zlooo.fixyou.session.SessionID.Fields.values())
        return result
    }

    private static char[] ca(String stringToChange) {
        stringToChange.toCharArray()
    }
}
