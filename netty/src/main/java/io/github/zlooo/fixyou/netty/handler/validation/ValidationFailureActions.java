package io.github.zlooo.fixyou.netty.handler.validation;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ValidationFailureActions {

    public static final ValidationFailureAction RELEASE_MESSAGE = (ctx, msg, fixMessageObjectPool) -> msg.release();
}
