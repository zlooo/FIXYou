package pl.zlooo.fixyou.netty.handler.validation;

import java.util.function.BiFunction;

public interface TwoArgsValidator<T, W> extends BiFunction<T, W, ValidationFailureAction>, Validator {
}
