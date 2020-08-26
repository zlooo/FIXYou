package io.github.zlooo.fixyou.session

import io.github.zlooo.fixyou.Resettable
import io.github.zlooo.fixyou.model.FixSpec
import spock.lang.Specification

class AbstractSessionStateTest extends Specification {

    private Resettable resettable1 = Mock()
    private Resettable resettable2 = Mock()
    private Resettable resettable3 = Mock()
    private AbstractSessionState sessionState = new TestSessionState(new SessionConfig(), new SessionID([] as char[], 0, [] as char[], 0, [] as char[], 0), null)

    def "should notify resettables about session reset"() {
        setup:
        sessionState.resettables.put("resettable1", resettable1)
        sessionState.resettables.put("resettable2", resettable2)
        sessionState.resettables.put("resettable3", resettable3)

        when:
        sessionState.reset()

        then:
        1 * resettable1.reset()
        1 * resettable2.reset()
        1 * resettable3.reset()
        0 * _
    }

    private static class TestSessionState extends AbstractSessionState {

        TestSessionState(SessionConfig sessionConfig, SessionID sessionId, FixSpec fixSpec) {
            super(sessionConfig, sessionId, fixSpec)
        }

        @Override
        void close() {
            //nothing to do
        }
    }
}