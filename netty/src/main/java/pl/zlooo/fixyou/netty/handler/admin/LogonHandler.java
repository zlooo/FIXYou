package pl.zlooo.fixyou.netty.handler.admin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.FixConstants;
import pl.zlooo.fixyou.fix.commons.Authenticator;
import pl.zlooo.fixyou.fix.commons.LogoutTexts;
import pl.zlooo.fixyou.fix.commons.RejectReasons;
import pl.zlooo.fixyou.fix.commons.session.SessionIDUtils;
import pl.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import pl.zlooo.fixyou.netty.handler.Handlers;
import pl.zlooo.fixyou.netty.handler.NamedHandler;
import pl.zlooo.fixyou.netty.handler.NettyResettablesNames;
import pl.zlooo.fixyou.netty.handler.SessionAwareChannelInboundHandler;
import pl.zlooo.fixyou.netty.handler.validation.ValidationOperations;
import pl.zlooo.fixyou.netty.utils.DelegatingChannelHandlerContext;
import pl.zlooo.fixyou.netty.utils.FixChannelListeners;
import pl.zlooo.fixyou.netty.utils.PipelineUtils;
import pl.zlooo.fixyou.parser.model.FixMessage;
import pl.zlooo.fixyou.parser.model.LongField;
import pl.zlooo.fixyou.session.SessionID;
import pl.zlooo.fixyou.session.SessionRegistry;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
class LogonHandler implements AdministrativeMessageHandler {

    @Nullable
    private final Authenticator authenticator;
    private final SessionRegistry<NettyHandlerAwareSessionState> sessionRegistry;
    private final ChannelHandler beforeSessionMessageValidatorHandler;
    private final ChannelHandler afterSessionMessageValidatorHandler;

    @Inject
    LogonHandler(@Nullable Authenticator authenticator, SessionRegistry sessionRegistry,
                 @NamedHandler(Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR) ChannelHandler beforeSessionMessageValidatorHandler,
                 @NamedHandler(Handlers.AFTER_SESSION_MESSAGE_VALIDATOR) ChannelHandler afterSessionMessageValidatorHandler) {
        this.authenticator = authenticator;
        this.sessionRegistry = sessionRegistry;
        this.beforeSessionMessageValidatorHandler = beforeSessionMessageValidatorHandler;
        this.afterSessionMessageValidatorHandler = afterSessionMessageValidatorHandler;
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
                sessionHandler.channelRead(notMovingForwardOnReadCtx.setDelegate(ctx), fixMessage);
            } catch (Exception e) {
                log.error("Exception happened when triggering read pipeline after session setup, closing connection", e);
                ctx.close();
            }
        }
    }

    private @Nullable
    SessionAwareChannelInboundHandler setupSessionAndSendResponse(FixMessage fixMessage, ChannelHandlerContext ctx) {
        final SessionID sessionID = SessionIDUtils.buildSessionID(fixMessage, true);
        log.debug("Received logon message for session {}", sessionID);
        final NettyHandlerAwareSessionState sessionState = sessionRegistry.getStateForSession(sessionID);
        if (sessionState != null) {
            if (!ValidationOperations.isValidLogonMessage(fixMessage)) { //Logon is a kind of special case, session is not yet established, validation handlers are not in pipeline yet hence special validation
                handleInvalidLogonMessage(fixMessage, ctx, sessionState);
            } else if (sessionState.getConnected().compareAndSet(false, true)) {
                if (authenticator == null || authenticator.isAuthenticated(fixMessage)) {
                    sessionState.setLogoutSent(false);
                    final boolean resetSequenceFlagSet = FixMessageUtils.hasBooleanFieldSet(fixMessage, FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER);
                    if (resetSequenceFlagSet) {
                        log.info("Reset sequence number flag set, resetting session {}", sessionState.getSessionId());
                        sessionState.reset();
                    }
                    final SessionAwareChannelInboundHandler sessionHandler = addRequiredHandlersToPipelineIfNeeded(ctx, sessionState, fixMessage.<LongField>getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).getValue());
                    if (!sessionState.isLogonSent()) {
                        ctx.writeAndFlush(
                                FixMessageUtils.toLogonMessage(sessionState.getFixMessageObjectPool().getAndRetain(),
                                                               sessionState.getFixSpec().applicationVersionId().getValue(),
                                                               fixMessage.<LongField>getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).getValue(),
                                                               fixMessage.<LongField>getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).getValue(), resetSequenceFlagSet))
                           .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).addListener(FixChannelListeners.LOGON_SENT);
                    } else {
                        log.debug("Logon has already been sent, not sending another one");
                    }
                    sessionState.getSessionConfig().getSessionStateListeners().forEach(sessionStateListener -> sessionStateListener.logOn(sessionState));
                    sessionState.setChannel(ctx.channel());
                    return sessionHandler;
                } else {
                    log.error("Authentication for session {} failed", sessionID);
                    fixMessage.retain();
                    ctx.writeAndFlush(FixMessageUtils.toLogoutMessage(fixMessage, LogoutTexts.BAD_CREDENTIALS))
                       .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                       .addListener(FixChannelListeners.LOGOUT_SENT);
                }
            } else {
                log.error("Second logon for session {} that's already set up, terminating connection", sessionID);
                ctx.channel().close();
            }
        } else {
            log.error("Logon message for unknown session {}, terminating connection", sessionID);
            ctx.channel().close();
        }
        return null;
    }

    @SneakyThrows
    private void handleInvalidLogonMessage(FixMessage fixMessage, ChannelHandlerContext ctx, NettyHandlerAwareSessionState sessionState) {
        log.warn("Invalid logon message received, responding with reject and logout messages");
        final ChannelOutboundHandler sessionHandler = (ChannelOutboundHandler) sessionState.getResettables().get(NettyResettablesNames.SESSION);
        final ChannelHandlerContext notMovingForwardCtx = (ChannelHandlerContext) sessionState.getResettables().get(NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT);
        final FixMessage rejectMessage = FixMessageUtils.toRejectMessage(sessionState.getFixMessageObjectPool().getAndRetain(), RejectReasons.OTHER, RejectReasons.INVALID_LOGON_MESSAGE);
        sessionHandler.write(notMovingForwardCtx, rejectMessage, null); //Yeah manually getting session handler and applying it looks a bit odd :/
        ctx.write(rejectMessage).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        final FixMessage logoutMessage = FixMessageUtils.toLogoutMessage(fixMessage, LogoutTexts.INVALID_LOGON_MESSAGE);
        logoutMessage.retain();
        sessionHandler.write(notMovingForwardCtx, logoutMessage, null);
        ctx.writeAndFlush(logoutMessage).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).addListener(ChannelFutureListener.CLOSE);
    }

    private SessionAwareChannelInboundHandler addRequiredHandlersToPipelineIfNeeded(ChannelHandlerContext ctx, NettyHandlerAwareSessionState sessionState, long heartbeatInterval) {
        if (ctx.pipeline().get(Handlers.SESSION.getName()) == null) {
            return PipelineUtils.addRequiredHandlersToPipeline(ctx.channel(), sessionState, beforeSessionMessageValidatorHandler, afterSessionMessageValidatorHandler, heartbeatInterval);
        } else {
            return null;
        }
    }

    @Override
    public char[] supportedMessageType() {
        return FixConstants.LOGON;
    }
}
