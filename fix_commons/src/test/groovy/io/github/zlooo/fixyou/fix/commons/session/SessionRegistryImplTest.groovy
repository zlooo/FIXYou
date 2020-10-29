package io.github.zlooo.fixyou.fix.commons.session

import io.github.zlooo.fixyou.FIXYouException
import io.github.zlooo.fixyou.Resettable
import io.github.zlooo.fixyou.model.FixSpec
import io.github.zlooo.fixyou.session.AbstractSessionState
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import org.assertj.core.api.Assertions
import spock.lang.Specification

import java.util.function.Function

class SessionRegistryImplTest extends Specification {

    private SessionID existingSessionId = new SessionID([] as char[], 0, [] as char[], 0, [] as char[], 0)
    private SessionRegistryImpl sessionRegistry = new SessionRegistryImpl()
    private TestSessionState existingSessionState = new TestSessionState(new SessionConfig(), existingSessionId, Mock(FixSpec))

    void setup() {
        sessionRegistry.@sessions.put(existingSessionId, existingSessionState)
    }

    def "should find state for given session id"() {
        expect:
        sessionRegistry.getStateForSession(existingSessionId) == existingSessionState
    }

    def "should not find not existing session state"() {
        expect:
        sessionRegistry.getStateForSession(new SessionID("beginString".toCharArray(), 5, "fakeSender".toCharArray(), 5, "fakeTarget".toCharArray(), 5)) == null
    }

    def "should find state for given session id when required"() {
        expect:
        sessionRegistry.getStateForSessionRequired(existingSessionId) == existingSessionState
    }

    def "should throw exception when cannot find required state"() {
        when:
        sessionRegistry.getStateForSessionRequired(new SessionID("beginString".toCharArray(), 5, "fakeSender".toCharArray(), 5, "fakeTarget".toCharArray(), 5))

        then:
        thrown(FIXYouException)
    }

    def "should register new session"() {
        setup:
        sessionRegistry.@sessions.clear()
        Resettable resettable1 = Mock()
        Resettable resettable2 = Mock()
        Function<TestSessionState, Map<String, Resettable>> resettableSupplier = { state -> [resetable1: resettable1, resetable2: resettable2] }

        when:
        sessionRegistry.registerExpectedSession(existingSessionState, resettableSupplier)

        then:
        Assertions.assertThat(sessionRegistry.@sessions).containsOnly(Assertions.entry(existingSessionId, existingSessionState))
        Assertions.assertThat(existingSessionState.resettables).containsOnly(Assertions.entry("resetable1", resettable1), Assertions.entry("resetable2", resettable2))
    }

    def "should not be able to register session using same id twice"() {
        setup:
        Resettable resettable1 = Mock()
        Resettable resettable2 = Mock()
        Function<TestSessionState, Map<String, Resettable>> resettableSupplier = { state -> [resetable1: resettable1, resetable2: resettable2] }

        when:
        sessionRegistry.registerExpectedSession(existingSessionState, resettableSupplier)

        then:
        thrown(FIXYouException)
        Assertions.assertThat(sessionRegistry.@sessions).containsOnly(Assertions.entry(existingSessionId, existingSessionState))
    }

    private static final class TestSessionState extends AbstractSessionState {
        TestSessionState(SessionConfig sessionConfig, SessionID sessionID, FixSpec fixSpec) {
            super(sessionConfig, sessionID, fixSpec)
        }
    }
}
