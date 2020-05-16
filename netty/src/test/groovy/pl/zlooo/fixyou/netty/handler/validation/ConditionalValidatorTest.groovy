package pl.zlooo.fixyou.netty.handler.validation

import pl.zlooo.fixyou.session.ValidationConfig
import spock.lang.Specification

import java.util.function.Predicate

class ConditionalValidatorTest extends Specification {

    private ConditionalValidator<String, String> conditionalValidator = new ConditionalValidator<String, String>() {}
    private Predicate<ValidationConfig> predicate1 = Mock()
    private TwoArgsValidator<String, String> validator1 = Mock()
    private PredicateWithValidator<TwoArgsValidator<String, String>> predicateWithValidator1 = new PredicateWithValidator<>(predicate1, validator1)
    private Predicate<ValidationConfig> predicate2 = Mock()
    private TwoArgsValidator<String, String> validator2 = Mock()
    private PredicateWithValidator<TwoArgsValidator<String, String>> predicateWithValidator2 = new PredicateWithValidator<>(predicate2, validator2)
    private ValidationConfig validationConfig = new ValidationConfig()

    def "should fire only one validator because of predicate"() {
        when:
        def result = conditionalValidator.fireConditionalValidators([predicateWithValidator1, predicateWithValidator2], validationConfig, "first", "second")

        then:
        result == null
        1 * predicate1.test(validationConfig) >> true
        1 * validator1.apply("first", "second")
        1 * predicate2.test(validationConfig) >> false
        0 * _
    }

    def "should stop validation chain when one validator returns error"() {
        setup:
        ValidationFailureAction validationFailureAction = Mock()

        when:
        def result = conditionalValidator.fireConditionalValidators([predicateWithValidator1, predicateWithValidator2], validationConfig, "first", "second")

        then:
        result == validationFailureAction
        1 * predicate1.test(validationConfig) >> true
        1 * validator1.apply("first", "second") >> validationFailureAction
        0 * _
    }
}
