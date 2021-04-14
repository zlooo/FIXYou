package io.github.zlooo.fixyou.netty.handler

import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.validation.PredicateWithValidator
import io.github.zlooo.fixyou.netty.handler.validation.SingleArgValidator
import io.github.zlooo.fixyou.netty.handler.validation.TwoArgsValidator
import io.github.zlooo.fixyou.netty.handler.validation.ValidationFailureAction
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.ValidationConfig
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.function.Predicate

class MessageValidatorHandlerTest extends Specification {


    private SingleArgValidator<FixMessage> validator1 = Mock()
    private Predicate<ValidationConfig> predicate2 = Mock()
    private TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState> validator2 = Mock()
    private PredicateWithValidator<TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState>> predicateWithValidator1 = new PredicateWithValidator<>(predicate2, validator2)
    private DefaultObjectPool<FixMessage> fixMessagePool = Mock()
    private MessageValidationHandler validatorHandler = new MessageValidationHandler([validator1], [predicateWithValidator1], fixMessagePool)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionAttribute = Mock()
    private NettyHandlerAwareSessionState sessionState = Mock()
    private ValidationConfig validationConfig = new ValidationConfig()
    private SessionConfig sessionConfig = new SessionConfig().setValidationConfig(validationConfig)
    @AutoCleanup
    private FixMessage fixMessage = new NotPoolableFixMessage()
    private ValidationFailureAction validationFailureAction = Mock()

    def "should perform after validation failure action when unconditional validation fails"() {
        when:
        validatorHandler.channelRead0(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.get() >> sessionState
        1 * validator1.apply(fixMessage) >> validationFailureAction
        1 * validationFailureAction.perform(channelHandlerContext, fixMessage, fixMessagePool)
        0 * _
    }

    def "should perform after validation failure action when conditional validation fails"() {
        when:
        validatorHandler.channelRead0(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.get() >> sessionState
        1 * validator1.apply(fixMessage)
        1 * sessionState.getSessionConfig() >> sessionConfig
        1 * predicate2.test(validationConfig) >> true
        1 * validator2.apply(fixMessage, sessionState) >> validationFailureAction
        1 * validationFailureAction.perform(channelHandlerContext, fixMessage, fixMessagePool)
        0 * _
    }

    def "should proceed when validation passes"() {
        when:
        validatorHandler.channelRead0(channelHandlerContext, fixMessage)

        then:
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.get() >> sessionState
        1 * validator1.apply(fixMessage)
        1 * sessionState.getSessionConfig() >> sessionConfig
        1 * predicate2.test(validationConfig) >> true
        1 * validator2.apply(fixMessage, sessionState)
        1 * channelHandlerContext.fireChannelRead(fixMessage)
        0 * _
    }
}
