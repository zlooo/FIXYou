package io.github.zlooo.fixyou.netty.handler.validation;

import io.github.zlooo.fixyou.session.ValidationConfig;
import lombok.Value;

import java.util.function.Predicate;

@Value
public class PredicateWithValidator<T extends Validator> {

    private final Predicate<ValidationConfig> validationPredicate;
    private final T validator;
}
