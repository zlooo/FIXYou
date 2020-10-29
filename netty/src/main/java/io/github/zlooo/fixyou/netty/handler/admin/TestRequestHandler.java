package io.github.zlooo.fixyou.netty.handler.admin;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.model.Field;
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

    private static final CharSequence SUPPORTED_MESSAGE_TYPE = String.copyValueOf(FixConstants.TEST_REQUEST);

    @Inject
    TestRequestHandler() {
    }

    @Override
    public void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("Test request received for session " + NettyHandlerAwareSessionState.getForChannelContext(ctx).getSessionId() + ", responding with heartbeat");
        }
        final Field testReqID = fixMessage.getField(FixConstants.TEST_REQ_ID_FIELD_NUMBER);
        ctx.writeAndFlush(FixMessageUtils.toHeartbeatMessage(fixMessage, testReqID.getCharArrayValue(), testReqID.getLength()).retain()).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    @Override
    public CharSequence supportedMessageType() {
        return SUPPORTED_MESSAGE_TYPE;
    }
}
