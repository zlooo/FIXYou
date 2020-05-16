package pl.zlooo.fixyou.netty.handler.admin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.FixConstants;
import pl.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import pl.zlooo.fixyou.parser.model.CharArrayField;
import pl.zlooo.fixyou.parser.model.FixMessage;

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
