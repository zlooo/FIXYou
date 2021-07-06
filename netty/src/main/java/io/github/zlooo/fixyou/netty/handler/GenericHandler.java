package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.fix.commons.RequiredFieldMissingException;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Clock;

@Singleton
@Slf4j
@ChannelHandler.Sharable
class GenericHandler extends ChannelDuplexHandler { //TODO remember about unit test for this class

    private final Clock clock;

    @Inject
    GenericHandler(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FixMessage) {
            final FixMessage fixMessage = (FixMessage) msg;
            final NettyHandlerAwareSessionState sessionState = NettyHandlerAwareSessionState.getForChannelContext(ctx);
            final CharSequence messageType = fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER);
            if ((sessionState == null || !sessionState.getConnected().get()) && !ArrayUtils.equals(FixConstants.LOGON, messageType)) {
                final Channel channel = ctx.channel();
                log.error("First message received but it's not logon request, disconnecting channel {}", channel);
                channel.close();
                return;
            }
            //TODO add message validation, optional depending on config
            //TODO check fix transport spec page 40 for details
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FixMessage) {
            final FixMessage fixMessage = (FixMessage) msg;
            //TODO make this optional, depending on config
            checkIfFieldsArePresent(fixMessage, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.SENDER_COMP_ID_FIELD_NUMBER, FixConstants.TARGET_COMP_ID_FIELD_NUMBER,
                                    FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER);
            final long timestamp = clock.millis();
            fixMessage.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, timestamp);
            if (shouldSetOrigSendingTime(fixMessage)) {
                fixMessage.setTimestampValue(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER, timestamp);
            }
        }
        super.write(ctx, msg, promise);
    }

    private boolean shouldSetOrigSendingTime(FixMessage fixMessage) {
        return FixMessageUtils.hasBooleanFieldSet(fixMessage, FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER) && !FixMessageUtils.hasField(fixMessage, FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        final Channel channel = ctx.channel();
        log.error("Exception caught, closing channel {}", channel, cause);
        channel.close();
    }

    private void checkIfFieldsArePresent(FixMessage fixMessage, int... numbersToCheck) {
        for (final int numberToCheck : numbersToCheck) {
            if (!fixMessage.isValueSet(numberToCheck)) {
                throw new RequiredFieldMissingException(numberToCheck, fixMessage);
            }
        }
    }
}
