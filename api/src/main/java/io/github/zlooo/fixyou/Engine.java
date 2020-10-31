package io.github.zlooo.fixyou;

import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.session.SessionConfig;
import io.github.zlooo.fixyou.session.SessionID;

import javax.annotation.Nonnull;
import java.util.concurrent.Future;

public interface Engine {

    @Nonnull
    Future<Void> start();

    @Nonnull
    Future<?> stop();

    /**
     * Registers new session
     *
     * @param sessionID     session identifier
     * @param fixSpec       instance of FixSpec that describes FIX message and fields structure. You can either implement one yourself or use
     *      *                      <a href="https://github.com/zlooo/FIXYou-tools/tree/master/fix_spec_generator">https://github.com/zlooo/FIXYou-tools/tree/master/fix_spec_generator</a> to generate one
     * @param sessionConfig session config
     * @return this
     */
    @Nonnull
    Engine registerSession(@Nonnull SessionID sessionID, @Nonnull FixSpec fixSpec, @Nonnull SessionConfig sessionConfig);
}
