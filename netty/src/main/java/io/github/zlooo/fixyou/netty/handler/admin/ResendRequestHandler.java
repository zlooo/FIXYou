package io.github.zlooo.fixyou.netty.handler.admin;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.session.SessionConfig;
import io.github.zlooo.fixyou.session.SessionID;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Slf4j
@Singleton
class ResendRequestHandler implements AdministrativeMessageHandler {

    private static final CharSequence SUPPORTED_MESSAGE_TYPE = String.copyValueOf(FixConstants.RESEND_REQUEST);

    private final ObjectPool<RetransmitionSubscriber> retransmissionSubscriberPool;
    private final ObjectPool<FixMessage> fixMessageObjectPool;

    @Inject
    ResendRequestHandler(@Named("retransmissionSubscriberPool") ObjectPool retransmissionSubscriberPool, @Named("fixMessageObjectPool") ObjectPool fixMessageObjectPool) {
        this.retransmissionSubscriberPool = retransmissionSubscriberPool;
        this.fixMessageObjectPool = fixMessageObjectPool;
    }

    @Override
    public void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx) {
        final long from = fixMessage.getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).getLongValue();
        final long to = fixMessage.getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).getLongValue();
        final NettyHandlerAwareSessionState sessionState = NettyHandlerAwareSessionState.getForChannelContext(ctx);
        final SessionID sessionId = sessionState.getSessionId();
        log.info("Resend request received for session {}, from {} to {}", sessionId, from, to);
        final SessionConfig sessionConfig = sessionState.getSessionConfig();
        if (sessionConfig.isPersistent()) {
            sessionConfig.getMessageStore()
                         .getMessages(sessionId, from, to, retransmissionSubscriberPool.getAndRetain().setChannelHandlerContext(ctx).setFixMessagePool(fixMessageObjectPool));
        } else {
            log.warn("Session {} does not have persistence configured, sending sequence reset - gap fill for whole requested range, <{}, {}>", sessionId, from, to);
            ctx.writeAndFlush(FixMessageUtils.toSequenceReset(fixMessage, from, to + 1, true).retain()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    @Override
    public CharSequence supportedMessageType() {
        return SUPPORTED_MESSAGE_TYPE;
    }
}
