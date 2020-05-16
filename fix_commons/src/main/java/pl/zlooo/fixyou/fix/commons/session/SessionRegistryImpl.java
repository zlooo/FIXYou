package pl.zlooo.fixyou.fix.commons.session;

import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.FIXYouException;
import pl.zlooo.fixyou.Resettable;
import pl.zlooo.fixyou.session.AbstractSessionState;
import pl.zlooo.fixyou.session.SessionID;
import pl.zlooo.fixyou.session.SessionRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Singleton
class SessionRegistryImpl<T extends AbstractSessionState> implements SessionRegistry<T> {
    private final Map<SessionID, T> sessions = new HashMap<>();

    @Nullable
    @Override
    public T getStateForSession(@Nonnull SessionID sessionID) {
        return sessions.get(sessionID);
    }

    @Nonnull
    @Override
    public T getStateForSessionRequired(@Nonnull SessionID sessionID) {
        final T sessionState = getStateForSession(sessionID);
        if (sessionState == null) {
            throw new FIXYouException("Could not find session for id " + sessionID);
        } else {
            return sessionState;
        }
    }

    @Override
    public void registerExpectedSession(@Nonnull T sessionState, @Nonnull Function<T, Map<String, Resettable>> resettableSupplier) {
        final Map<String, Resettable> resettables = sessionState.getResettables();
        resettables.clear();
        final SessionID sessionId = sessionState.getSessionId();
        log.debug("Preallocating channel handlers for session {}", sessionId);
        resettables.putAll(resettableSupplier.apply(sessionState));
        if (sessions.putIfAbsent(sessionId, sessionState) != null) {
            throw new FIXYouException("Session with id " + sessionId + " is already registered");
        }
    }

    @Nonnull
    @Override
    public Collection<T> getAll() {
        return sessions.values();
    }
}
