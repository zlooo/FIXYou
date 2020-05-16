package pl.zlooo.fixyou.netty.handler.admin;

import io.netty.channel.ChannelHandlerContext;
import pl.zlooo.fixyou.parser.model.FixMessage;

public interface AdministrativeMessageHandler {

    void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx);

    char[] supportedMessageType();
}
