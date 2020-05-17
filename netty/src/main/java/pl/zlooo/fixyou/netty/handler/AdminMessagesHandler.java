package pl.zlooo.fixyou.netty.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.commons.utils.ArrayUtils;
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import pl.zlooo.fixyou.netty.handler.admin.AdministrativeMessageHandler;
import pl.zlooo.fixyou.FixConstants;
import pl.zlooo.fixyou.parser.model.CharArrayField;
import pl.zlooo.fixyou.parser.model.FixMessage;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;

@Slf4j
@Singleton
@ChannelHandler.Sharable
class AdminMessagesHandler extends SimpleChannelInboundHandler<FixMessage> {

    //TODO check the web for fancier collection
    private final TreeMap<char[], AdministrativeMessageHandler> handlerMap = new TreeMap<>(ArrayUtils::compare);

    @Inject
    AdminMessagesHandler(Set<AdministrativeMessageHandler> administrativeMessageHandlers) {
        for (final AdministrativeMessageHandler administrativeMessageHandler : administrativeMessageHandlers) {
            final AdministrativeMessageHandler previousHandler = handlerMap.put(administrativeMessageHandler.supportedMessageType(), administrativeMessageHandler);
            if (previousHandler != null) {
                throw new IllegalArgumentException("Looks like we've got multiple handlers for " + Arrays.toString(
                        previousHandler.supportedMessageType()) + ", please check your code, there is supposed to be only 1 handler per message type");
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FixMessage fixMessage) throws Exception {
        final char[] messageType = fixMessage.<CharArrayField>getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).getValue();
        if (isSessionIsEstablished(channelHandlerContext) || Arrays.equals(messageType, FixConstants.LOGON)) {
            final AdministrativeMessageHandler messageHandler = handlerMap.get(messageType);
            if (messageHandler != null) {
                messageHandler.handleMessage(fixMessage, channelHandlerContext);
            } else {
                fixMessage.retain();
                if (log.isDebugEnabled()) {
                    log.debug("No admin handler found for message {}, passing it further", Arrays.toString(messageType));
                }
                channelHandlerContext.fireChannelRead(fixMessage);
            }
        }
    }

    private boolean isSessionIsEstablished(ChannelHandlerContext channelHandlerContext) {
        return NettyHandlerAwareSessionState.getForChannelContext(channelHandlerContext) != null;
    }
}
