package pl.zlooo.fixyou.fix.commons.session

import org.assertj.core.api.Assertions
import pl.zlooo.fixyou.FIXYouException
import pl.zlooo.fixyou.Resettable
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool
import pl.zlooo.fixyou.model.FixSpec
import pl.zlooo.fixyou.parser.model.FixMessage
import pl.zlooo.fixyou.session.SessionConfig
import pl.zlooo.fixyou.session.SessionID
import spock.lang.Specification

import java.util.function.Function

class SessionRegistryImplTest extends Specification {

    private SessionID existingSessionId = new SessionID([] as char[], [] as char[], [] as char[])
    private SessionRegistryImpl sessionRegistry = new SessionRegistryImpl()
    private TestSessionState existingSessionState = new TestSessionState(new SessionConfig(), existingSessionId, Mock(DefaultObjectPool), Mock(FixSpec))

    void setup() {
        sessionRegistry.@sessions.put(existingSessionId, existingSessionState)
    }

    def "should find state for given session id"() {
        expect:
        sessionRegistry.getStateForSession(existingSessionId) == existingSessionState
    }

    def "should not find not existing session state"() {
        expect:
        sessionRegistry.getStateForSession(new SessionID("beginString".toCharArray(), "fakeSender".toCharArray(), "fakeTarget".toCharArray())) == null
    }

    def "should find state for given session id when required"() {
        expect:
        sessionRegistry.getStateForSessionRequired(existingSessionId) == existingSessionState
    }

    def "should throw exception when cannot find required state"() {
        when:
        sessionRegistry.getStateForSessionRequired(new SessionID("beginString".toCharArray(), "fakeSender".toCharArray(), "fakeTarget".toCharArray()))

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

    private static final class TestSessionState extends AbstractMessagePoolingSessionState {
        TestSessionState(SessionConfig sessionConfig, SessionID sessionID, DefaultObjectPool<FixMessage> fixMessageObjectPool, FixSpec fixSpec) {
            super(sessionConfig, sessionID, fixMessageObjectPool, fixSpec)
        }
    }
}
