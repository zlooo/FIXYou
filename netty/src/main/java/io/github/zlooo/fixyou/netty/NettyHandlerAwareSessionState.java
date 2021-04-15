package io.github.zlooo.fixyou.netty;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.model.ExtendedFixSpec;
import io.github.zlooo.fixyou.netty.handler.NettyResettablesNames;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.session.AbstractSessionState;
import io.github.zlooo.fixyou.session.SessionConfig;
import io.github.zlooo.fixyou.session.SessionID;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.util.AttributeKey;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Slf4j
public class NettyHandlerAwareSessionState extends AbstractSessionState {

    public static final AttributeKey<NettyHandlerAwareSessionState> ATTRIBUTE_KEY = AttributeKey.valueOf("sessionState");
    private static final String SESSION_MISSING_EXCEPTION_MESSAGE_PART_2 = " to have session state, but it's not there";

    private Channel channel;

    public NettyHandlerAwareSessionState(SessionConfig sessionConfig, SessionID sessionId, ExtendedFixSpec fixSpec) {
        super(sessionConfig, sessionId, fixSpec);
    }

    public static @Nullable
    NettyHandlerAwareSessionState getForChannel(@Nonnull Channel channel) {
        return channel.attr(ATTRIBUTE_KEY).get();
    }

    public static @Nullable
    NettyHandlerAwareSessionState getForChannelContext(@Nonnull ChannelHandlerContext channelHandlerContext) {
        return channelHandlerContext.channel().attr(ATTRIBUTE_KEY).get();
    }

    public static SessionID getSessionID(ChannelHandlerContext channelHandlerContext) {
        return channelHandlerContext.channel().attr(ATTRIBUTE_KEY).get().getSessionId();
    }

    @SneakyThrows
    public void queueMessage(FixMessage fixMessage) {
        final ChannelHandlerContext notMovingForwardOnReadAndWriteCtx = (ChannelHandlerContext) getResettables().get(NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT);
        try {
            ((ChannelOutboundHandler) getResettables().get(NettyResettablesNames.SESSION)).write(notMovingForwardOnReadAndWriteCtx, fixMessage, null);
            if (getSessionConfig().isPersistent()) {
                getSessionConfig().getMessageStore().storeMessage(getSessionId(), fixMessage.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER), fixMessage);
            }
            log.debug("Queueing message {}", fixMessage);
        } finally {
            fixMessage.release();
        }
    }
}
