package io.github.zlooo.fixyou.fix.commons.session

import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import org.assertj.core.api.Assertions
import spock.lang.Specification

import java.util.function.Function

class SessionRegistryImplTest extends Specification {

    private io.github.zlooo.fixyou.session.SessionID existingSessionId = new io.github.zlooo.fixyou.session.SessionID([] as char[], [] as char[], [] as char[])
    private SessionRegistryImpl sessionRegistry = new SessionRegistryImpl()
    private TestSessionState existingSessionState = new TestSessionState(new io.github.zlooo.fixyou.session.SessionConfig(), existingSessionId, Mock(DefaultObjectPool), Mock(io.github.zlooo.fixyou.model.FixSpec))

    void setup() {
        sessionRegistry.@sessions.put(existingSessionId, existingSessionState)
    }

    def "should find state for given session id"() {
        expect:
        sessionRegistry.getStateForSession(existingSessionId) == existingSessionState
    }

    def "should not find not existing session state"() {
        expect:
        sessionRegistry.getStateForSession(new io.github.zlooo.fixyou.session.SessionID("beginString".toCharArray(), "fakeSender".toCharArray(), "fakeTarget".toCharArray())) == null
    }

    def "should find state for given session id when required"() {
        expect:
        sessionRegistry.getStateForSessionRequired(existingSessionId) == existingSessionState
    }

    def "should throw exception when cannot find required state"() {
        when:
        sessionRegistry.getStateForSessionRequired(new io.github.zlooo.fixyou.session.SessionID("beginString".toCharArray(), "fakeSender".toCharArray(), "fakeTarget".toCharArray()))

        then:
        thrown(io.github.zlooo.fixyou.FIXYouException)
    }

    def "should register new session"() {
        setup:
        sessionRegistry.@sessions.clear()
        io.github.zlooo.fixyou.Resettable resettable1 = Mock()
        io.github.zlooo.fixyou.Resettable resettable2 = Mock()
        Function<TestSessionState, Map<String, io.github.zlooo.fixyou.Resettable>> resettableSupplier = { state -> [resetable1: resettable1, resetable2: resettable2] }

        when:
        sessionRegistry.registerExpectedSession(existingSessionState, resettableSupplier)

        then:
        Assertions.assertThat(sessionRegistry.@sessions).containsOnly(Assertions.entry(existingSessionId, existingSessionState))
        Assertions.assertThat(existingSessionState.resettables).containsOnly(Assertions.entry("resetable1", resettable1), Assertions.entry("resetable2", resettable2))
    }

    def "should not be able to register session using same id twice"() {
        setup:
        io.github.zlooo.fixyou.Resettable resettable1 = Mock()
        io.github.zlooo.fixyou.Resettable resettable2 = Mock()
        Function<TestSessionState, Map<String, io.github.zlooo.fixyou.Resettable>> resettableSupplier = { state -> [resetable1: resettable1, resetable2: resettable2] }

        when:
        sessionRegistry.registerExpectedSession(existingSessionState, resettableSupplier)

        then:
        thrown(io.github.zlooo.fixyou.FIXYouException)
        Assertions.assertThat(sessionRegistry.@sessions).containsOnly(Assertions.entry(existingSessionId, existingSessionState))
    }

    private static final class TestSessionState extends AbstractMessagePoolingSessionState {
        TestSessionState(io.github.zlooo.fixyou.session.SessionConfig sessionConfig, io.github.zlooo.fixyou.session.SessionID sessionID, DefaultObjectPool<io.github.zlooo.fixyou.parser.model.FixMessage> fixMessageObjectPool, io.github
                .zlooo.fixyou.model.FixSpec fixSpec) {
            super(sessionConfig, sessionID, fixMessageObjectPool, fixSpec)
        }
    }
}
