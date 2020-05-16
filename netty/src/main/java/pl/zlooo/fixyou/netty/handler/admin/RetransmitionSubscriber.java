package pl.zlooo.fixyou.netty.handler.admin;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.FixConstants;
import pl.zlooo.fixyou.commons.pool.AbstractPoolableObject;
import pl.zlooo.fixyou.commons.pool.DefaultObjectPool;
import pl.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import pl.zlooo.fixyou.parser.model.*;
import pl.zlooo.fixyou.session.LongSubscriber;

@Slf4j
@ToString(callSuper = true)
@Accessors(chain = true)
class RetransmitionSubscriber extends AbstractPoolableObject implements LongSubscriber<FixMessage> {

    private static final long NOT_SET = -1;
    @Setter
    private ChannelHandlerContext channelHandlerContext;
    @Setter
    private DefaultObjectPool<FixMessage> fixMessagePool;
    private long fromValue = NOT_SET;
    private long toValue = NOT_SET;

    @Override
    public void onSubscribe() {
        if (channelHandlerContext == null) {
            throw new IllegalStateException("Subscription started but no channel handler context set? Dude get your shit together");
        }
        if (fixMessagePool == null) {
            throw new IllegalStateException("Subscription started but no fix message pool set? Dude get your shit together");
        }
    }

    @Override
    public void onNext(long sequenceNumber, FixMessage fixMessage) {
        if (fixMessage == FixMessageUtils.EMPTY_FAKE_MESSAGE) {
            storeSequenceNumberForGapFill(sequenceNumber);
            fixMessage.release();
        } else {
            if (!FixMessageUtils.isAdminMessage(fixMessage)) {
                sendGapFillIfNecessary();
                fixMessage.<BooleanField>getField(FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER).setValue(true);
                final CharArrayField origSendingTimeField = fixMessage.getField(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER);
                origSendingTimeField.reset();
                final AbstractField sendingTimeField = fixMessage.getField(FixConstants.SENDING_TIME_FIELD_NUMBER);
                if (sendingTimeField.isValueSet()) {
                    origSendingTimeField.setFieldData(sendingTimeField.getFieldData());
                }
                channelHandlerContext.write(fixMessage).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
            } else {
                //let's not spam other party with single sequence gap fills but check if we can send one which will fill multiple sequence numbers
                final long sequenceValue = fixMessage.<LongField>getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).getValue();
                storeSequenceNumberForGapFill(sequenceValue);
                fixMessage.release();
            }
        }
    }

    private void storeSequenceNumberForGapFill(long sequenceValue) {
        if (fromValue == NOT_SET) {
            fromValue = sequenceValue;
        }
        toValue = sequenceValue;
    }

    private void sendGapFillIfNecessary() {
        if (fromValue != NOT_SET && toValue != NOT_SET) {
            channelHandlerContext.write(FixMessageUtils.toSequenceReset(fixMessagePool.getAndRetain(), fromValue, toValue + 1, true));
            fromValue = NOT_SET;
            toValue = NOT_SET;
        }
    }

    @Override
    public void onError(Throwable throwable) {
        sendGapFillIfNecessary();
        channelHandlerContext.flush();
        log.error("Exception while processing resend request", throwable);
        release();
    }

    @Override
    public void onComplete() {
        sendGapFillIfNecessary();
        channelHandlerContext.flush();
        log.info("Resend request filled");
        release();
    }

    @Override
    protected void deallocate() {
        channelHandlerContext = null;
        fixMessagePool = null;
        fromValue = NOT_SET;
        toValue = NOT_SET;
        super.deallocate();
    }

    @Override
    public void close() {
        //nothing to do here
    }
}
