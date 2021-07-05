package io.github.zlooo.fixyou.fix.commons;


import io.github.zlooo.fixyou.model.FixMessage;

public interface Authenticator {
    boolean isAuthenticated(FixMessage msg);
}
