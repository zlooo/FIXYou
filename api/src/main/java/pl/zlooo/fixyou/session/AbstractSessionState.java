package pl.zlooo.fixyou.session;

import lombok.Data;
import lombok.experimental.Accessors;
import pl.zlooo.fixyou.Closeable;
import pl.zlooo.fixyou.Resettable;
import pl.zlooo.fixyou.model.FixSpec;

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
}
