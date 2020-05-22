package io.github.zlooo.fixyou.fix.commons.session;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.session.AbstractSessionState;
import io.github.zlooo.fixyou.session.SessionConfig;
import io.github.zlooo.fixyou.session.SessionID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

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
