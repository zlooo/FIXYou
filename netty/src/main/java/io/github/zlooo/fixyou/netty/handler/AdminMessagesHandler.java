package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.utils.Comparators;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.netty.handler.admin.AdministrativeMessageHandler;
import io.github.zlooo.fixyou.parser.model.CharSequenceField;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;
import java.util.TreeMap;

@Slf4j
@Singleton
@ChannelHandler.Sharable
class AdminMessagesHandler extends SimpleChannelInboundHandler<FixMessage> {

    //TODO check the web for fancier collection
    private final TreeMap<CharSequence, AdministrativeMessageHandler> handlerMap = new TreeMap<>(Comparators::compare);

    @Inject
    AdminMessagesHandler(Set<AdministrativeMessageHandler> administrativeMessageHandlers) {
        for (final AdministrativeMessageHandler administrativeMessageHandler : administrativeMessageHandlers) {
            final AdministrativeMessageHandler previousHandler = handlerMap.put(administrativeMessageHandler.supportedMessageType(), administrativeMessageHandler);
            if (previousHandler != null) {
                throw new IllegalArgumentException("Looks like we've got multiple handlers for " + previousHandler.supportedMessageType() + ", please check your code, there is supposed to be only 1 handler per message type");
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FixMessage fixMessage) throws Exception {
        final CharSequence messageType = fixMessage.<CharSequenceField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).getValue();
        if (isSessionIsEstablished(channelHandlerContext) || ArrayUtils.equals(FixConstants.LOGON, messageType)) {
            final AdministrativeMessageHandler messageHandler = handlerMap.get(messageType);
            if (messageHandler != null) {
                messageHandler.handleMessage(fixMessage, channelHandlerContext);
            } else {
                fixMessage.retain();
                if (log.isDebugEnabled()) {
                    log.debug("No admin handler found for message {}, passing it further", messageType);
                }
                channelHandlerContext.fireChannelRead(fixMessage);
            }
        }
    }

    private boolean isSessionIsEstablished(ChannelHandlerContext channelHandlerContext) {
        return NettyHandlerAwareSessionState.getForChannelContext(channelHandlerContext) != null;
    }
}
