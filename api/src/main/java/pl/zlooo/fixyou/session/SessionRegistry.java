package pl.zlooo.fixyou.session;


import pl.zlooo.fixyou.Resettable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public interface SessionRegistry<T extends AbstractSessionState> {
    @Nullable
    T getStateForSession(@Nonnull SessionID sessionID);

    @Nonnull
    T getStateForSessionRequired(@Nonnull SessionID sessionID);

    void registerExpectedSession(@Nonnull T sessionState, @Nonnull Function<T, Map<String, Resettable>> resettableSupplier);

    @Nonnull
    Collection<T> getAll();
}
