package io.github.zlooo.fixyou.netty.handler.admin;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.model.CharArrayField;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
@ChannelHandler.Sharable
class TestRequestHandler implements AdministrativeMessageHandler {

    @Inject
    TestRequestHandler() {
    }

    @Override
    public void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("Test request received for session " + NettyHandlerAwareSessionState.getForChannelContext(ctx).getSessionId() + ", responding with heartbeat");
        }
        fixMessage.retain();
        ctx.writeAndFlush(FixMessageUtils.toHeartbeatMessage(fixMessage, fixMessage.<CharArrayField>getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER).getValue())).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public char[] supportedMessageType() {
        return FixConstants.TEST_REQUEST;
    }
}
