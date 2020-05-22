package io.github.zlooo.fixyou.netty.handler.validation;

import io.github.zlooo.fixyou.session.ValidationConfig;

import java.util.List;

public interface ConditionalValidator<T, W> {

    default ValidationFailureAction fireConditionalValidators(List<PredicateWithValidator<TwoArgsValidator<T, W>>> predicateWithValidators, ValidationConfig validationConfig,
                                                              T firstArg, W secondArg) {
        for (final PredicateWithValidator<TwoArgsValidator<T, W>> predicateWithValidator : predicateWithValidators) {
            if (predicateWithValidator.getValidationPredicate().test(validationConfig)) {
                final ValidationFailureAction conditionalValidationResult = predicateWithValidator.getValidator().apply(firstArg, secondArg);
                if (conditionalValidationResult != null) {
                    return conditionalValidationResult;
                }
            }
        }
        return null;
    }
}
