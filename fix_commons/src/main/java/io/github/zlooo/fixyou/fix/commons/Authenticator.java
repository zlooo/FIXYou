package io.github.zlooo.fixyou.fix.commons;


import io.github.zlooo.fixyou.parser.model.FixMessage;

public interface Authenticator {
    boolean isAuthenticated(FixMessage msg);
}
