package io.github.zlooo.fixyou.session;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.model.FixSpec;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Accessors(chain = true)
public abstract class AbstractSessionState implements Resettable, Closeable {

    private final SessionConfig sessionConfig;
    private final SessionID sessionId;
    private final FixSpec fixSpec;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private boolean logonSent;
    private boolean logoutSent;
    private final Map<String, Resettable> resettables = new HashMap<>();

    @Override
    public void reset() {
        resettables.forEach((key, value) -> value.reset());
    }

    @Override
    public void close() {
        for (final Resettable resettable : getResettables().values()) {
            if (resettable instanceof Closeable) {
                ((Closeable) resettable).close();
            }
        }
    }
}
