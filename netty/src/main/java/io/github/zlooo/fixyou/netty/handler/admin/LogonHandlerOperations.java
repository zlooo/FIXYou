package io.github.zlooo.fixyou.netty.handler.admin;

import io.github.zlooo.fixyou.commons.AbstractPoolableFixMessage;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.commons.utils.DateUtils;
import io.github.zlooo.fixyou.fix.commons.LogoutTexts;
import io.github.zlooo.fixyou.fix.commons.RejectReasons;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.netty.handler.NettyResettablesNames;
import io.github.zlooo.fixyou.netty.utils.FixChannelListeners;
import io.github.zlooo.fixyou.session.StartStopConfig;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Slf4j
@UtilityClass
class LogonHandlerOperations {

    static boolean checkSessionStartTime(StartStopConfig startStopConfig, ChannelHandlerContext ctx, Clock clock, ObjectPool<? extends AbstractPoolableFixMessage> fixMessageObjectPool) {
        if (startStopConfig != StartStopConfig.INFINITE) {
            final LocalDate date = LocalDate.now(clock);
            final long sessionStartTime = DateUtils.epochMillis(date, startStopConfig.getStartDay(), startStopConfig.getStartTime(), true);
            final DayOfWeek stopDay = startStopConfig.getStopDay();
            long sessionStopTime = DateUtils.epochMillis(date, stopDay, startStopConfig.getStopTime(), true);
            final long previousSessionStopTime;
            if (sessionStopTime < sessionStartTime) {
                previousSessionStopTime = sessionStopTime;
                sessionStopTime += stopDay == null ? DateUtils.MILLIS_IN_DAY : DateUtils.MILLIS_IN_WEEK;
            } else {
                previousSessionStopTime = 0;
            }
            final long nowMillis = clock.millis();
            if (sessionStopTime < nowMillis || (sessionStartTime > nowMillis && nowMillis > previousSessionStopTime)) {
                ctx.writeAndFlush(FixMessageUtils.toLogoutMessage(fixMessageObjectPool.getAndRetain(), LogoutTexts.LOGON_OUTSIDE_SESSION_ACTIVE_TIME))
                   .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                   .addListener(FixChannelListeners.LOGOUT_SENT);
                return false;
            } else {
                ctx.executor()
                   .schedule(() -> ctx.writeAndFlush(FixMessageUtils.toLogoutMessage(fixMessageObjectPool.getAndRetain(), null))
                                      .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
                                      .addListener(FixChannelListeners.LOGOUT_SENT), sessionStopTime - nowMillis, TimeUnit.MILLISECONDS);
            }
        }
        return true;
    }

    @SneakyThrows
    static void handleInvalidLogonMessage(FixMessage fixMessage, ChannelHandlerContext ctx, NettyHandlerAwareSessionState sessionState, ObjectPool<? extends AbstractPoolableFixMessage> fixMessageObjectPool) {
        log.warn("Invalid logon message received, responding with reject and logout messages");
        final ChannelOutboundHandler sessionHandler = (ChannelOutboundHandler) sessionState.getResettables().get(NettyResettablesNames.SESSION);
        final ChannelHandlerContext notMovingForwardCtx = (ChannelHandlerContext) sessionState.getResettables().get(NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT);
        final FixMessage rejectMessage = FixMessageUtils.toRejectMessage(fixMessageObjectPool.getAndRetain(), RejectReasons.OTHER, RejectReasons.INVALID_LOGON_MESSAGE);
        sessionHandler.write(notMovingForwardCtx, rejectMessage, null); //Yeah manually getting session handler and applying it looks a bit odd :/
        ctx.write(rejectMessage).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        final FixMessage logoutMessage = FixMessageUtils.toLogoutMessage(fixMessage, LogoutTexts.INVALID_LOGON_MESSAGE);
        sessionHandler.write(notMovingForwardCtx, logoutMessage, null);
        ctx.writeAndFlush(ReferenceCountUtil.retain(logoutMessage)).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).addListener(ChannelFutureListener.CLOSE);
    }
}
