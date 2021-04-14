package io.github.zlooo.fixyou.fix.commons.session

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.utils.Comparators
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage
import io.github.zlooo.fixyou.session.SessionID
import org.assertj.core.api.Assertions
import spock.lang.Specification

class SessionIDUtilsTest extends Specification {

    private static SessionID sessionID = new SessionID('FIXT.1.1', 'senderCompID', 'targetCompID')

    def "should set all session id fields"() {
        setup:
        def fixMessage = new NotPoolableFixMessage()

        when:
        SessionIDUtils.setSessionIdFields(fixMessage, sessionID)

        then:
        def sessionIdFieldsMap = sessionIDFieldsMap()
        for (Map.Entry<SessionID.Fields, Integer> mappingEntry : sessionIdFieldsMap) {
            assert Comparators.compare(fixMessage.getCharSequenceValue(mappingEntry.value), sessionID.properties[mappingEntry.key.name()]) == 0
        }

        cleanup:
        fixMessage?.close()
    }

    def "should build session id from fix message"() {
        setup:
        def fixMessage = new NotPoolableFixMessage()
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, sessionID.beginString)
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, sessionID.senderCompID)
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, sessionID.targetCompID)

        expect:
        SessionIDUtils.buildSessionID(fixMessage, false) == sessionID

        cleanup:
        fixMessage?.close()
    }

    def "should build session id from fix message with comp ids flipped"() {
        setup:
        def fixMessage = new NotPoolableFixMessage()
        fixMessage.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, sessionID.beginString)
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, sessionID.senderCompID)
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, sessionID.targetCompID)

        expect:
        SessionIDUtils.buildSessionID(fixMessage, true) == new SessionID(sessionID.beginString, sessionID.targetCompID, sessionID.senderCompID)

        cleanup:
        fixMessage?.close()
    }

    def "should check if comp ids are equal"() {
        setup:
        def fixMessage = new NotPoolableFixMessage()
        fixMessage.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, senderCompID)
        fixMessage.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, targetCompID)

        expect:
        SessionIDUtils.checkCompIDs(fixMessage, sessionIdToCompare, flipIDs) == result

        cleanup:
        fixMessage?.close()

        where:
        senderCompID | targetCompID | flipIDs | sessionIdToCompare                  | result
        'scid'       | 'tcid'       | false   | new SessionID('bs', 'scid', 'tcid') | true
        'scid1'      | 'tcid'       | false   | new SessionID('bs', 'scid', 'tcid') | false
        'scid'       | 'tcid1'      | false   | new SessionID('bs', 'scid', 'tcid') | false
        'tcid'       | 'scid'       | true    | new SessionID('bs', 'scid', 'tcid') | true
        'tcid1'      | 'scid'       | true    | new SessionID('bs', 'scid', 'tcid') | false
        'tcid'       | 'scid1'      | true    | new SessionID('bs', 'scid', 'tcid') | false
    }

    Map<SessionID.Fields, Integer> sessionIDFieldsMap() {
        def result = [(SessionID.Fields.beginString) : FixConstants.BEGIN_STRING_FIELD_NUMBER,
                      (SessionID.Fields.senderCompID): FixConstants.SENDER_COMP_ID_FIELD_NUMBER,
                      (SessionID.Fields.targetCompID): FixConstants.TARGET_COMP_ID_FIELD_NUMBER]
        Assertions.assertThat(result).doesNotContainValue(null).containsOnlyKeys(SessionID.Fields.values())
        return result
    }
}
