package pl.zlooo.fixyou.fix.commons.session;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.Closeable;
import pl.zlooo.fixyou.Resettable;
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool;
import pl.zlooo.fixyou.model.FixSpec;
import pl.zlooo.fixyou.parser.model.FixMessage;
import pl.zlooo.fixyou.session.AbstractSessionState;
import pl.zlooo.fixyou.session.SessionConfig;
import pl.zlooo.fixyou.session.SessionID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
@Slf4j
public abstract class AbstractMessagePoolingSessionState extends AbstractSessionState implements Resettable {


    private final DefaultObjectPool<FixMessage> fixMessageObjectPool;

    public AbstractMessagePoolingSessionState(SessionConfig sessionConfig, SessionID sessionId, DefaultObjectPool<FixMessage> fixMessageObjectPool,
                                              FixSpec fixSpec) {
        super(sessionConfig, sessionId, fixSpec);
        this.fixMessageObjectPool = fixMessageObjectPool;
    }

    public void reset() {
        super.reset();
        if (!fixMessageObjectPool.areAllObjectsReturned()) {
            log.warn("Not all fix messages have been returned to pool, session details {}", this);
        }
    }

    @Override
    public void close() {
        for (final Resettable resettable : getResettables().values()) {
            if (resettable instanceof Closeable) {
                ((Closeable) resettable).close();
            }
        }
        fixMessageObjectPool.close();
    }
}
