package pl.zlooo.fixyou.netty.handler.validation;

import lombok.Value;
import pl.zlooo.fixyou.session.ValidationConfig;

import java.util.function.Predicate;

@Value
public class PredicateWithValidator<T extends Validator> {

    private final Predicate<ValidationConfig> validationPredicate;
    private final T validator;
}
