package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.session.MessageStore;
import io.github.zlooo.fixyou.session.SessionID;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
class MessageStoreHandler extends ChannelOutboundHandlerAdapter implements Resettable {

    private final SessionID sessionID;
    private final MessageStore<FixMessage> messageStore;

    MessageStoreHandler(SessionID sessionId, MessageStore<FixMessage> messageStore) {
        this.sessionID = sessionId;
        this.messageStore = messageStore;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        final NettyHandlerAwareSessionState sessionState = NettyHandlerAwareSessionState.getForChannelContext(ctx);
        if (sessionState != null && msg instanceof FixMessage) {
            final FixMessage fixMessage = (FixMessage) msg;
            messageStore.storeMessage(sessionState.getSessionId(), fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).getLongValue(), fixMessage);
        } else {
            log.warn("Either session cannot be found or handler is in wrong place(expecting to get FixMessage but got {})", msg);
        }
        ctx.write(msg, promise);
    }

    @Override
    public void reset() {
        messageStore.reset(sessionID);
    }
}
