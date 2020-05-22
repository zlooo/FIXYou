package io.github.zlooo.fixyou.fix.commons.session


import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.fix.commons.TestSpec
import spock.lang.Specification

class AbstractMessagePoolingSessionStateTest extends Specification {

    private DefaultObjectPool<io.github.zlooo.fixyou.parser.model.FixMessage> fixMessageDefaultObjectPool = Mock()
    private io.github.zlooo.fixyou.Resettable resettable = Mock()
    private ClosableResettable closableResettable = Mock()
    private AbstractMessagePoolingSessionState sessionState = new TestSessionState(new io.github.zlooo.fixyou.session.SessionConfig(), new io.github.zlooo.fixyou.session.SessionID([] as char[], [] as char[], [] as char[]), fixMessageDefaultObjectPool, TestSpec.INSTANCE)

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

        TestSessionState(io.github.zlooo.fixyou.session.SessionConfig sessionConfig, io.github.zlooo.fixyou.session.SessionID sessionId, DefaultObjectPool<io.github.zlooo.fixyou.parser.model.FixMessage> fixMessageObjectPool, io.github.zlooo.fixyou.model.FixSpec fixSpec) {
            super(sessionConfig, sessionId, fixMessageObjectPool, fixSpec)
        }
    }

    private static class ClosableResettable implements io.github.zlooo.fixyou.Resettable, io.github.zlooo.fixyou.Closeable {

        @Override
        void close() {

        }

        @Override
        void reset() {

        }
    }
}
