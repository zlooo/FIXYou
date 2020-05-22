package io.github.zlooo.fixyou.netty.handler.admin;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.parser.model.LongField;
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

    private final ObjectPool<RetransmitionSubscriber> retransmissionSubscriberPool;

    @Inject
    ResendRequestHandler(@Named("retransmissionSubscriberPool") ObjectPool retransmissionSubscriberPool) {
        this.retransmissionSubscriberPool = retransmissionSubscriberPool;
    }

    @Override
    public void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx) {
        final long from = fixMessage.<LongField>getField(FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER).getValue();
        final long to = fixMessage.<LongField>getField(FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER).getValue();
        final NettyHandlerAwareSessionState sessionState = NettyHandlerAwareSessionState.getForChannelContext(ctx);
        final SessionID sessionId = sessionState.getSessionId();
        log.info("Resend request received for session {}, from {} to {}", sessionId, from, to);
        final SessionConfig sessionConfig = sessionState.getSessionConfig();
        if (sessionConfig.isPersistent()) {
            sessionConfig.getMessageStore()
                         .getMessages(sessionId, from, to, retransmissionSubscriberPool.getAndRetain().setChannelHandlerContext(ctx).setFixMessagePool(sessionState.getFixMessageObjectPool()));
        } else {
            log.warn("Session {} does not have persistence configured, sending sequence reset - gap fill for whole requested range, <{}, {}>", sessionId, from, to);
            fixMessage.retain();
            ctx.writeAndFlush(FixMessageUtils.toSequenceReset(fixMessage, from, to + 1, true)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
    }

    @Override
    public char[] supportedMessageType() {
        return FixConstants.RESEND_REQUEST;
    }
}
