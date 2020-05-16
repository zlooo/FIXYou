package pl.zlooo.fixyou.netty.handler.validation;

import java.util.List;

public interface UnconditionalValidator<T> {

    default ValidationFailureAction fireUnconditionalValidators(List<SingleArgValidator<T>> unconditionalValidators, T objectToValidate) {
        for (final SingleArgValidator<T> validator : unconditionalValidators) {
            final ValidationFailureAction validationResult = validator.apply(objectToValidate);
            if (validationResult != null) {
                return validationResult;
            }
        }
        return null;
    }
}
