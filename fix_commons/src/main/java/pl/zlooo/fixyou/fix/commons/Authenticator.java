package pl.zlooo.fixyou.fix.commons;


import pl.zlooo.fixyou.parser.model.FixMessage;

public interface Authenticator {
    boolean isAuthenticated(FixMessage msg);
}
