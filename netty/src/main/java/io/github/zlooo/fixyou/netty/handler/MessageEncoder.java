package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.commons.utils.NumberConstants;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.netty.utils.FixSpec50SP2;
import io.github.zlooo.fixyou.parser.cache.FieldNumberCache;
import io.github.zlooo.fixyou.parser.model.Field;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.concurrent.EventExecutor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

@Slf4j
@Singleton
@ChannelHandler.Sharable
class MessageEncoder extends MessageToByteEncoder<FixMessage> {

    private static final int SINGLE_DIGIT_ENC_FIELD_NUMBER_LENGTH = 2;
    private static final int CHECKSUM_VALUE_LENGTH = 3;
    private static final int MAX_BODY_LENGTH_FIELD_LENGTH = 8; //on one hand we should -1 it because it can only be positive value but on the other we have to terminate this field with SOH so it evens out
    private static final FixSpec BACKUP_SPEC = new FixSpec50SP2();

    private final FieldNumberCache fieldNumberCache;

    @Inject
    MessageEncoder(FieldNumberCache fieldNumberCache) {
        this.fieldNumberCache = fieldNumberCache;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, FixMessage msg, ByteBuf out) {
        final Field beginString = msg.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER);
        final Field bodyLength = msg.getField(FixConstants.BODY_LENGTH_FIELD_NUMBER);
        final int afterBodyLengthIndex = SINGLE_DIGIT_ENC_FIELD_NUMBER_LENGTH + beginString.getLength() + SINGLE_DIGIT_ENC_FIELD_NUMBER_LENGTH + MAX_BODY_LENGTH_FIELD_LENGTH + 2;
        out.writerIndex(afterBodyLengthIndex);
        final NettyHandlerAwareSessionState sessionState = NettyHandlerAwareSessionState.getForChannelContext(ctx);
        final FixSpec fixSpec = sessionState != null ? sessionState.getFixSpec() : BACKUP_SPEC; //it may happen that session is not yet established at this point, for example reject as a response to logon message
        final int[] fieldsOrder = fixSpec.getFieldsOrder();
        int sumOfBytes = 0;
        final EventExecutor executor = ctx.executor();
        for (int i = 2; i < fieldsOrder.length - 1; i++) {
            final Field field = msg.getFieldOrPlaceholder(ArrayUtils.getElementAt(fieldsOrder, i));
            if (field.isValueSet()) {
                final FieldNumberCache.ByteBufWithSum encodedFieldNumber = fieldNumberCache.getEncodedFieldNumber(field.getNumber(), executor);
                out.writeBytes(encodedFieldNumber.getByteBuf().readerIndex(0));
                sumOfBytes += encodedFieldNumber.getSumOfBytes();
                sumOfBytes += field.appendByteBufWithValue(out, fixSpec, executor) + FixMessage.FIELD_SEPARATOR;
                out.writeByte(FixMessage.FIELD_SEPARATOR);
            }
        }
        sumOfBytes += prependTwoFirstFields(out, afterBodyLengthIndex, beginString, bodyLength, fixSpec, executor);
        appendWithChecksum(out, msg.getField(FixConstants.CHECK_SUM_FIELD_NUMBER), sumOfBytes, executor);
        if (log.isDebugEnabled()) {
            log.debug("Encoded message " + out.toString(StandardCharsets.US_ASCII) + " buffer " + out);
        }
    }

    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, FixMessage msg, boolean preferDirect) {
        return ctx.alloc().ioBuffer(DefaultConfiguration.DEFAULT_OUT_MESSAGE_BUF_INIT_CAPACITY);
    }

    private int prependTwoFirstFields(ByteBuf out, int afterBodyLengthIndex, Field beginString, Field bodyLength, FixSpec fixSpec, Executor executor) {
        final int bodyLengthValue = out.writerIndex() - afterBodyLengthIndex;
        int powerOfTenIndex = 0;
        for (; powerOfTenIndex < NumberConstants.POWERS_OF_TEN.length; powerOfTenIndex++) {
            if (NumberConstants.POWERS_OF_TEN[powerOfTenIndex] > bodyLengthValue) {
                break;
            }
        }
        final int startingIndex = afterBodyLengthIndex - SINGLE_DIGIT_ENC_FIELD_NUMBER_LENGTH - beginString.getLength() - SINGLE_DIGIT_ENC_FIELD_NUMBER_LENGTH - powerOfTenIndex - 2;
        out.markWriterIndex();
        out.readerIndex(startingIndex).writerIndex(startingIndex);
        final FieldNumberCache.ByteBufWithSum beginStringEncodedFieldNumber = fieldNumberCache.getEncodedFieldNumber(beginString.getNumber(), executor);
        out.writeBytes(beginStringEncodedFieldNumber.getByteBuf().readerIndex(0));
        int sumOfBytes = beginStringEncodedFieldNumber.getSumOfBytes();
        sumOfBytes += beginString.appendByteBufWithValue(out, fixSpec, executor) + FixMessage.FIELD_SEPARATOR;
        out.writeByte(FixMessage.FIELD_SEPARATOR);
        final FieldNumberCache.ByteBufWithSum bodyLengthEncodedFieldNumber = fieldNumberCache.getEncodedFieldNumber(bodyLength.getNumber(), executor);
        out.writeBytes(bodyLengthEncodedFieldNumber.getByteBuf().readerIndex(0));
        sumOfBytes += bodyLengthEncodedFieldNumber.getSumOfBytes();
        bodyLength.setLongValue(bodyLengthValue);
        sumOfBytes += bodyLength.appendByteBufWithValue(out, fixSpec, executor) + FixMessage.FIELD_SEPARATOR;
        out.writeByte(FixMessage.FIELD_SEPARATOR).resetWriterIndex();
        return sumOfBytes;
    }

    private void appendWithChecksum(ByteBuf out, Field checksumField, int sumOfBytes, EventExecutor executor) {
        final FieldNumberCache.ByteBufWithSum checksumEncodedFieldNumber = fieldNumberCache.getEncodedFieldNumber(checksumField.getNumber(), executor);
        out.writeBytes(checksumEncodedFieldNumber.getByteBuf().readerIndex(0));
        final int checksum = sumOfBytes & FixConstants.CHECK_SUM_MODULO_MASK;
        FieldUtils.writeEncoded(checksum, out, CHECKSUM_VALUE_LENGTH);
        out.writeByte(FixMessage.FIELD_SEPARATOR);
    }
}
