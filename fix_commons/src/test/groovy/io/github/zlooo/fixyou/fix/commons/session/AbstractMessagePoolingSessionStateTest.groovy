package io.github.zlooo.fixyou.fix.commons.session

import io.github.zlooo.fixyou.Resettable
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.fix.commons.TestSpec
import io.github.zlooo.fixyou.model.FixSpec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import spock.lang.Specification

class AbstractMessagePoolingSessionStateTest extends Specification {

    private DefaultObjectPool<FixMessage> fixMessageDefaultObjectPool = Mock()
    private Resettable resettable = Mock()
    private ClosableResettable closableResettable = Mock()
    private AbstractMessagePoolingSessionState sessionState = new TestSessionState(new SessionConfig(), new SessionID([] as char[], 0, [] as char[], 0, [] as char[], 0), fixMessageDefaultObjectPool, TestSpec.INSTANCE)

    void setup() {
        sessionState.getResettables()["resettable"] = resettable
        sessionState.getResettables()["closeableResettable"] = closableResettable
    }

    def "should close resettables and pool when closed"() {
        when:
        sessionState.close()

        then:
        1 * fixMessageDefaultObjectPool.close()
        1 * closableResettable.close()
        0 * _
    }

    def "should check message pool when session is reset"() {
        when:
        sessionState.reset()

        then:
        1 * fixMessageDefaultObjectPool.areAllObjectsReturned()
        1 * resettable.reset()
        1 * closableResettable.reset()
        0 * _
    }

    private static final class TestSessionState extends AbstractMessagePoolingSessionState {

        TestSessionState(SessionConfig sessionConfig, SessionID sessionId, DefaultObjectPool<FixMessage> fixMessageObjectPool, FixSpec fixSpec) {
            super(sessionConfig, sessionId, fixMessageObjectPool, fixSpec)
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
