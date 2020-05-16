package pl.zlooo.fixyou.netty.handler;

import javax.inject.Qualifier;

@Qualifier
public @interface NamedHandler {

    Handlers value();
}
