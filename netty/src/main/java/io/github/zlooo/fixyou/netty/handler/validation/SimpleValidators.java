package io.github.zlooo.fixyou.netty.handler.validation;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.fix.commons.RejectReasons;
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.channel.ChannelFutureListener;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class SimpleValidators {

    public static final SingleArgValidator<FixMessage> ORIG_SENDING_TIME_PRESENT = fixMsg -> {
        if (FixMessageUtils.hasBooleanFieldSet(fixMsg, FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER) && !FixMessageUtils.hasField(fixMsg, FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER)) {
            log.warn("Received message with PosDupFlag set but without OrigSendingTime, rejecting it");
            return (ctx, fixMessage, fixMessageObjectPool) -> ctx.writeAndFlush(FixMessageUtils.toRejectMessage(fixMsg, RejectReasons.REQUIRED_TAG_MISSING, FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER).retain())
                                                                 .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        } else {
            return null;
        }
    };
}
