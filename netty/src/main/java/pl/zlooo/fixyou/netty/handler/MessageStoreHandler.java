package pl.zlooo.fixyou.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.FixConstants;
import pl.zlooo.fixyou.Resettable;
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import pl.zlooo.fixyou.parser.model.FixMessage;
import pl.zlooo.fixyou.parser.model.LongField;
import pl.zlooo.fixyou.session.MessageStore;
import pl.zlooo.fixyou.session.SessionID;

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
            messageStore.storeMessage(sessionState.getSessionId(), fixMessage.<LongField>getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).getValue(), fixMessage);
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
