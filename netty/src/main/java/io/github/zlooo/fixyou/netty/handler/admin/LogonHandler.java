package io.github.zlooo.fixyou.netty.handler.admin;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.AbstractPoolableFixMessage;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.fix.commons.Authenticator;
import io.github.zlooo.fixyou.fix.commons.LogoutTexts;
import io.github.zlooo.fixyou.fix.commons.session.SessionIDUtils;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.netty.handler.Handlers;
import io.github.zlooo.fixyou.netty.handler.NamedHandler;
import io.github.zlooo.fixyou.netty.handler.NettyResettablesNames;
import io.github.zlooo.fixyou.netty.handler.SessionAwareChannelInboundHandler;
import io.github.zlooo.fixyou.netty.handler.validation.ValidationOperations;
import io.github.zlooo.fixyou.netty.utils.DelegatingChannelHandlerContext;
import io.github.zlooo.fixyou.netty.utils.FixChannelListeners;
import io.github.zlooo.fixyou.netty.utils.PipelineUtils;
import io.github.zlooo.fixyou.session.SessionID;
import io.github.zlooo.fixyou.session.SessionRegistry;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Clock;

@Singleton
@Slf4j
class LogonHandler implements AdministrativeMessageHandler {

    private static final CharSequence SUPPORTED_MESSAGE_TYPE = String.copyValueOf(FixConstants.LOGON);

    @Nullable
    private final Authenticator authenticator;
    private final SessionRegistry<NettyHandlerAwareSessionState> sessionRegistry;
    private final ChannelHandler beforeSessionMessageValidatorHandler;
    private final ChannelHandler afterSessionMessageValidatorHandler;
    private final ObjectPool<? extends AbstractPoolableFixMessage> fixMessageObjectPool;
    private final Clock clock;

    @Inject
    LogonHandler(@Nullable Authenticator authenticator, SessionRegistry sessionRegistry,
                 @NamedHandler(Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR) ChannelHandler beforeSessionMessageValidatorHandler,
                 @NamedHandler(Handlers.AFTER_SESSION_MESSAGE_VALIDATOR) ChannelHandler afterSessionMessageValidatorHandler,
                 @Named("fixMessageObjectPool") ObjectPool<? extends AbstractPoolableFixMessage> fixMessageObjectPool,
                 Clock clock) {
        this.authenticator = authenticator;
        this.sessionRegistry = sessionRegistry;
        this.beforeSessionMessageValidatorHandler = beforeSessionMessageValidatorHandler;
        this.afterSessionMessageValidatorHandler = afterSessionMessageValidatorHandler;
        this.fixMessageObjectPool = fixMessageObjectPool;
        this.clock = clock;
    }

    @Override
    public void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx) {
        final SessionAwareChannelInboundHandler sessionHandler = setupSessionAndSendResponse(fixMessage, ctx);
        if (sessionHandler != null) {
            try {
                final NettyHandlerAwareSessionState sessionState = sessionHandler.getSessionState();
                final DelegatingChannelHandlerContext notMovingForwardOnReadCtx =
                        (DelegatingChannelHandlerContext) sessionState.getResettables().get(NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT);
                //this is to check if logon had expected sequence number and send resend request if necessary
                sessionHandler.channelRead(notMovingForwardOnReadCtx.setDelegate(ctx.pipeline().context(Handlers.SESSION.getName())), fixMessage);
            } catch (Exception e) {
                log.error("Exception happened when triggering read pipeline after session setup, closing connection", e);
                ctx.close();
            }
        }
    }

    private SessionAwareChannelInboundHandler setupSessionAndSendResponse(FixMessage fixMessage, ChannelHandlerContext ctx) {
        final SessionID sessionID = SessionIDUtils.buildSessionID(fixMessage, true);
        log.debug("Received logon message for session {}", sessionID);
        final NettyHandlerAwareSessionState sessionState = sessionRegistry.getStateForSession(sessionID);
        if (sessionState == null) {
            log.error("Logon message for unknown session {}, terminating connection", sessionID);
            ctx.channel().close();
        } else {
            if (!ValidationOperations.isValidLogonMessage(fixMessage)) { //Logon is a kind of special case, session is not yet established, validation handlers are not in pipeline yet hence special validation
                LogonHandlerOperations.handleInvalidLogonMessage(fixMessage, ctx, sessionState, fixMessageObjectPool);
            } else {
                if (!LogonHandlerOperations.checkSessionStartTime(sessionState.getSessionConfig().getStartStopConfig(), ctx, clock, fixMessageObjectPool)) {
                    log.error("Logon to {} outside session active time", sessionID);
                } else if (sessionState.getConnected().compareAndSet(false, true)) {
                    if (authenticator == null || authenticator.isAuthenticated(fixMessage)) {
                        sessionState.setLogoutSent(false);
                        final boolean resetSequenceFlagSet = FixMessageUtils.hasBooleanFieldSet(fixMessage, FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER);
                        if (resetSequenceFlagSet) {
                            log.info("Reset sequence number flag set, resetting session {}", sessionState.getSessionId());
                            sessionState.reset();
                        }
                        final SessionAwareChannelInboundHandler sessionHandler = addRequiredHandlersToPipelineIfNeeded(ctx, sessionState, fixMessage.getLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER));
                        sendLogonResponseIfNotSentAlready(fixMessage, ctx, sessionState, resetSequenceFlagSet);
                        sessionState.getSessionConfig().getSessionStateListeners().forEach(sessionStateListener -> sessionStateListener.logOn(sessionState));
                        sessionState.setChannel(ctx.channel());
                        return sessionHandler;
                    } else {
                        log.error("Authentication for session {} failed", sessionID);
                        ctx.writeAndFlush(ReferenceCountUtil.retain(FixMessageUtils.toLogoutMessage(fixMessage, LogoutTexts.BAD_CREDENTIALS)))
                           .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                           .addListener(FixChannelListeners.LOGOUT_SENT);
                    }
                } else {
                    log.error("Second logon for session {} that's already set up, terminating connection", sessionID);
                    ctx.channel().close();
                }
            }
        }
        return null;
    }

    private void sendLogonResponseIfNotSentAlready(FixMessage fixMessage, ChannelHandlerContext ctx, NettyHandlerAwareSessionState sessionState, boolean resetSequenceFlagSet) {
        if (!sessionState.isLogonSent()) {
            ctx.writeAndFlush(
                       FixMessageUtils.toLogonMessage(fixMessageObjectPool.getAndRetain(),
                                                      sessionState.getFixSpec().applicationVersionId().getValue(),
                                                      fixMessage.getLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER),
                                                      fixMessage.getLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER),
                                                      resetSequenceFlagSet))
               .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).addListener(FixChannelListeners.LOGON_SENT);
        } else {
            log.debug("Logon has already been sent, not sending another one");
        }
    }

    private SessionAwareChannelInboundHandler addRequiredHandlersToPipelineIfNeeded(ChannelHandlerContext ctx, NettyHandlerAwareSessionState sessionState, long heartbeatInterval) {
        if (ctx.pipeline().get(Handlers.SESSION.getName()) == null) {
            return PipelineUtils.addRequiredHandlersToPipeline(ctx.channel(), sessionState, beforeSessionMessageValidatorHandler, afterSessionMessageValidatorHandler, heartbeatInterval);
        } else {
            return null;
        }
    }

    @Override
    public CharSequence supportedMessageType() {
        return SUPPORTED_MESSAGE_TYPE;
    }
}
