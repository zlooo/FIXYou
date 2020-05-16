package pl.zlooo.fixyou.netty.handler.admin;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.FixConstants;
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import pl.zlooo.fixyou.parser.model.FixMessage;
import pl.zlooo.fixyou.parser.model.LongField;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
class RejectHandler implements AdministrativeMessageHandler {

    @Inject
    RejectHandler() {
    }

    @Override
    public void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx) {
        log.warn("Received reject message for session {}, sequence number of rejected message {}. Whole message will be logged on debug level", NettyHandlerAwareSessionState.getForChannelContext(ctx).getSessionId(),
                 fixMessage.<LongField>getField(FixConstants.REFERENCED_SEQUENCE_NUMBER_FIELD_NUMBER));
        log.debug("Reject message received {}", fixMessage);
    }

    @Override
    public char[] supportedMessageType() {
        return FixConstants.REJECT;
    }
}