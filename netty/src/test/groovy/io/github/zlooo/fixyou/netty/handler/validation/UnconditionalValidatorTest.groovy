package io.github.zlooo.fixyou.netty.handler.validation

import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.SimpleFixMessage
import spock.lang.Specification

class UnconditionalValidatorTest extends Specification {

    private UnconditionalValidator unconditionalValidator = new UnconditionalValidator() {}
    private SingleArgValidator<FixMessage> validator1 = Mock()
    private SingleArgValidator<FixMessage> validator2 = Mock()
    private FixMessage fixMessage = new SimpleFixMessage()

    def "should return null validation failure action when validation passes"() {
        when:
        def result = unconditionalValidator.fireUnconditionalValidators([validator1, validator2], fixMessage)

        then:
        result == null
        1 * validator1.apply(fixMessage)

        then:
        1 * validator2.apply(fixMessage)
        0 * _
    }

    def "should stop validation chain when one validator returns error"() {
        setup:
        ValidationFailureAction validationFailureAction = Mock()

        when:
        def result = unconditionalValidator.fireUnconditionalValidators([validator1, validator2], fixMessage)

        then:
        result == validationFailureAction
        1 * validator1.apply(fixMessage) >> validationFailureAction
        0 * _
    }
}
