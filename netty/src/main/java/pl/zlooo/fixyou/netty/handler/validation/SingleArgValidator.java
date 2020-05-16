package pl.zlooo.fixyou.netty.handler.validation;

import java.util.function.Function;

public interface SingleArgValidator<T> extends Function<T, ValidationFailureAction>, Validator {
}
