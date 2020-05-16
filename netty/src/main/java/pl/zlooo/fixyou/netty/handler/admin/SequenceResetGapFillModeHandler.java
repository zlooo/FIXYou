package pl.zlooo.fixyou.netty.handler.admin;

import io.netty.channel.ChannelHandlerContext;
import pl.zlooo.fixyou.FixConstants;
import pl.zlooo.fixyou.parser.model.FixMessage;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
class SequenceResetGapFillModeHandler implements AdministrativeMessageHandler {

    @Inject
    SequenceResetGapFillModeHandler() {
    }

    @Override
    public void handleMessage(FixMessage fixMessage, ChannelHandlerContext ctx) {
        //nothing to do
    }

    @Override
    public char[] supportedMessageType() {
        return FixConstants.SEQUENCE_RESET;
    }
}
