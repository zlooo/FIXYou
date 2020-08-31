package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.netty.handler.validation.*;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@ChannelHandler.Sharable
class MessageValidationHandler extends SimpleChannelInboundHandler<FixMessage>
        implements UnconditionalValidator<FixMessage>, ConditionalValidator<FixMessage, NettyHandlerAwareSessionState> {

    private final List<SingleArgValidator<FixMessage>> unconditionalValidators;
    private final List<PredicateWithValidator<TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState>>> predicateWithValidators;

    MessageValidationHandler(List<SingleArgValidator<FixMessage>> unconditionalValidators,
                             List<PredicateWithValidator<TwoArgsValidator<FixMessage, NettyHandlerAwareSessionState>>> predicateWithValidators) {
        super(false);
        this.unconditionalValidators = unconditionalValidators;
        this.predicateWithValidators = predicateWithValidators;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FixMessage msg) throws Exception {
        final NettyHandlerAwareSessionState sessionState = NettyHandlerAwareSessionState.getForChannelContext(ctx);
        final ValidationFailureAction validationFailureAction = checkFixMessage(msg, sessionState);
        if (validationFailureAction != null) {
            log.warn("Message validation failed, performing {}", validationFailureAction);
            validationFailureAction.perform(ctx, msg, sessionState.getFixMessageWritePool());
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private ValidationFailureAction checkFixMessage(FixMessage message, NettyHandlerAwareSessionState sessionState) {
        final ValidationFailureAction unconditionalValidatorsResult = fireUnconditionalValidators(this.unconditionalValidators, message);
        if (unconditionalValidatorsResult != null) {
            return unconditionalValidatorsResult;
        }
        return fireConditionalValidators(this.predicateWithValidators, sessionState.getSessionConfig().getValidationConfig(), message, sessionState);
    }
}
