package pl.zlooo.fixyou.fix.commons.session

import pl.zlooo.fixyou.Closeable
import pl.zlooo.fixyou.Resettable
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool
import pl.zlooo.fixyou.fix.commons.TestSpec
import pl.zlooo.fixyou.model.FixSpec
import pl.zlooo.fixyou.parser.model.FixMessage
import pl.zlooo.fixyou.session.SessionConfig
import pl.zlooo.fixyou.session.SessionID
import spock.lang.Specification

class AbstractMessagePoolingSessionStateTest extends Specification {

    private DefaultObjectPool<FixMessage> fixMessageDefaultObjectPool = Mock()
    private Resettable resettable = Mock()
    private ClosableResettable closableResettable = Mock()
    private AbstractMessagePoolingSessionState sessionState = new TestSessionState(new SessionConfig(), new SessionID([] as char[], [] as char[], [] as char[]), fixMessageDefaultObjectPool, TestSpec.INSTANCE)

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

    private static class ClosableResettable implements Resettable, Closeable {

        @Override
        void close() {

        }

        @Override
        void reset() {

        }
    }
}
