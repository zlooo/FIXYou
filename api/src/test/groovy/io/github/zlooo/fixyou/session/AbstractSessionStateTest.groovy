package io.github.zlooo.fixyou.session

import io.github.zlooo.fixyou.Resettable
import io.github.zlooo.fixyou.model.FixSpec
import spock.lang.Specification

class AbstractSessionStateTest extends Specification {

    private Resettable resettable1 = Mock()
    private Resettable resettable2 = Mock()
    private Resettable resettable3 = Mock()
    private AbstractSessionState sessionState = new TestSessionState(new SessionConfig(), new SessionID("","",""), null)

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

    def "should close resettables when closed"() {
        setup:
        ClosableResettable closableResettable = Mock()
        sessionState.resettables["resettable"] = resettable1
        sessionState.resettables["closeableResettable"] = closableResettable

        when:
        sessionState.close()

        then:
        1 * closableResettable.close()
        0 * _
    }

    private static class TestSessionState extends AbstractSessionState {

        TestSessionState(SessionConfig sessionConfig, SessionID sessionId, FixSpec fixSpec) {
            super(sessionConfig, sessionId, fixSpec)
        }
    }

    private static class ClosableResettable implements Resettable, io.github.zlooo.fixyou.Closeable {

        @Override
        void close() {

        }

        @Override
        void reset() {

        }
    }
}
