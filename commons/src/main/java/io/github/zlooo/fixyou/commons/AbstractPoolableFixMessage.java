package io.github.zlooo.fixyou.commons;

import io.github.zlooo.fixyou.Copyable;
import io.github.zlooo.fixyou.commons.pool.AbstractPoolableObject;
import io.github.zlooo.fixyou.model.FixMessage;

public abstract class AbstractPoolableFixMessage<T extends AbstractPoolableFixMessage<T>> extends AbstractPoolableObject implements FixMessage, Copyable<T> {
}
