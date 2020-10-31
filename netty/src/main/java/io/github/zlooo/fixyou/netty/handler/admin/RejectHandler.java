package io.github.zlooo.fixyou.netty.handler.admin;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
class RejectHandler implements AdministrativeMessageHandler {

    private static final CharSequence SUPPORTED_MESSAGE_TYPE = String.copyValueOf(FixConstants.REJECT);

    @Inject
    RejectHandler() {
    }

    @Override
    public void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx) {
        log.warn("Received reject message for session {}, sequence number of rejected message {}. Whole message will be logged on debug level", NettyHandlerAwareSessionState.getForChannelContext(ctx).getSessionId(),
                 fixMessage.getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER));
        log.debug("Reject message received {}", fixMessage);
    }

    @Override
    public CharSequence supportedMessageType() {
        return SUPPORTED_MESSAGE_TYPE;
    }
}
