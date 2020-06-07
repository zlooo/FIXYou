package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.ReusableCharArray;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.commons.utils.NumberConstants;
import io.github.zlooo.fixyou.netty.utils.ValueAddingByteProcessor;
import io.github.zlooo.fixyou.parser.model.AbstractField;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.parser.model.LongField;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
abstract class AbstractMessageEncoder extends MessageToByteEncoder<FixMessage> {

    private static final int MAX_BOODY_LENGTH_FIELD_LENGTH = LongField.FIELD_DATA_LENGTH; //on one hand we should -1 it because it can only be positive value but on the other we have to terminate this field with SOH so it evens out

    @Override
    protected void encode(ChannelHandlerContext ctx, FixMessage msg, ByteBuf out) {
        final AbstractField[] fieldsOrdered = msg.getFieldsOrdered();
        final AbstractField beginString = fieldsOrdered[0];
        final LongField bodyLength = (LongField) fieldsOrdered[0];
        final int afterBodyLengthIndex = beginString.getEncodedFieldNumber().writerIndex() + beginString.getLength() + bodyLength.getEncodedFieldNumber().writerIndex() + MAX_BOODY_LENGTH_FIELD_LENGTH;
        out.writerIndex(afterBodyLengthIndex);
        for (int i = 2; i < fieldsOrdered.length - 1; i++) {
            final AbstractField field = fieldsOrdered[i];
            if (field.isValueSet()) {
                out.writeBytes(field.getEncodedFieldNumber());
                field.appendByteBufWithValue(out);
                out.writeByte(FixMessage.FIELD_SEPARATOR_BYTE);
            }
        }
        prependTwoFirstFields(out, afterBodyLengthIndex, beginString, bodyLength);
        appendWithChecksum(out, msg.getField(FixConstants.CHECK_SUM_FIELD_NUMBER), getValueAddingByteProcessor());
        if (log.isDebugEnabled()) {
            log.debug("Encoded message " + out.toString(StandardCharsets.US_ASCII));
        }
    }

    private void prependTwoFirstFields(ByteBuf out, int afterBodyLengthIndex, AbstractField beginString, LongField bodyLength) {
        final int bodyLengthValue = out.writerIndex() - afterBodyLengthIndex;
        int powerOfTenIndex = 0;
        for (; powerOfTenIndex < NumberConstants.POWERS_OF_TEN.length; powerOfTenIndex++) {
            if (NumberConstants.POWERS_OF_TEN[powerOfTenIndex] > bodyLengthValue) {
                powerOfTenIndex--;
                break;
            }
        }
        final int startingIndex = afterBodyLengthIndex - beginString.getEncodedFieldNumber().writerIndex() - beginString.getLength() - bodyLength.getEncodedFieldNumber().writerIndex() - powerOfTenIndex;
        out.markWriterIndex();
        out.readerIndex(startingIndex).writerIndex(startingIndex).writeBytes(beginString.getEncodedFieldNumber());
        beginString.appendByteBufWithValue(out);
        out.writeByte(FixMessage.FIELD_SEPARATOR_BYTE);
        out.writeBytes(bodyLength.getEncodedFieldNumber());
        bodyLength.setValue(bodyLengthValue);
        bodyLength.appendByteBufWithValue(out);
        out.writeByte(FixMessage.FIELD_SEPARATOR_BYTE).resetWriterIndex();
    }

    protected abstract ValueAddingByteProcessor getValueAddingByteProcessor();

    private static void appendWithChecksum(ByteBuf out, AbstractField checksumField, ValueAddingByteProcessor valueAddingByteProcessor) {
        try {
            out.forEachByte(valueAddingByteProcessor);
            final ReusableCharArray checksumEncoded =
                    FieldUtils.toCharSequenceWithSpecifiedSizeAndDefaultValue(valueAddingByteProcessor.getResult() % FixConstants.CHECK_SUM_MODULO, 3, '0');
            out.writeBytes(checksumField.getEncodedFieldNumber()).writeCharSequence(checksumEncoded, StandardCharsets.US_ASCII);
            ReferenceCountUtil.release(checksumEncoded);
            out.writeByte(FixMessage.FIELD_SEPARATOR_BYTE);
        } finally {
            valueAddingByteProcessor.reset();
        }
    }
}
