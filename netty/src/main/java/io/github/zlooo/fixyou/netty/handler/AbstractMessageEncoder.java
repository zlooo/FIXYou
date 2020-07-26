package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.commons.utils.NumberConstants;
import io.github.zlooo.fixyou.netty.utils.ValueAddingByteProcessor;
import io.github.zlooo.fixyou.parser.model.AbstractField;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.parser.model.LongField;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
abstract class AbstractMessageEncoder extends MessageToByteEncoder<FixMessage> {

    private static final int CHECKSUM_VALUE_LENGTH = 3;
    private static final int MAX_BOODY_LENGTH_FIELD_LENGTH = LongField.FIELD_DATA_LENGTH; //on one hand we should -1 it because it can only be positive value but on the other we have to terminate this field with SOH so it evens out

    @Override
    protected void encode(ChannelHandlerContext ctx, FixMessage msg, ByteBuf out) {
        final AbstractField[] fieldsOrdered = msg.getFieldsOrdered();
        final AbstractField beginString = fieldsOrdered[0];
        final LongField bodyLength = (LongField) fieldsOrdered[1];
        final int afterBodyLengthIndex = beginString.getEncodedFieldNumberLength() + beginString.getLength() + bodyLength.getEncodedFieldNumberLength() + MAX_BOODY_LENGTH_FIELD_LENGTH;
        out.writerIndex(afterBodyLengthIndex);
        for (int i = 2; i < fieldsOrdered.length - 1; i++) {
            final AbstractField field = fieldsOrdered[i];
            if (field.isValueSet()) {
                field.appendFieldNumber(out);
                field.appendByteBufWithValue(out);
                out.writeByte(FixMessage.FIELD_SEPARATOR);
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
                break;
            }
        }
        final int startingIndex = afterBodyLengthIndex - beginString.getEncodedFieldNumberLength() - beginString.getLength() - bodyLength.getEncodedFieldNumberLength() - powerOfTenIndex - 2;
        out.markWriterIndex();
        out.readerIndex(startingIndex).writerIndex(startingIndex);
        beginString.appendFieldNumber(out);
        beginString.appendByteBufWithValue(out);
        out.writeByte(FixMessage.FIELD_SEPARATOR);
        bodyLength.appendFieldNumber(out);
        bodyLength.setValue(bodyLengthValue);
        bodyLength.appendByteBufWithValue(out);
        out.writeByte(FixMessage.FIELD_SEPARATOR).resetWriterIndex();
    }

    protected abstract ValueAddingByteProcessor getValueAddingByteProcessor();

    private static void appendWithChecksum(ByteBuf out, AbstractField checksumField, ValueAddingByteProcessor valueAddingByteProcessor) {
        try {
            out.forEachByte(valueAddingByteProcessor);
            checksumField.appendFieldNumber(out);
            final int checksum = valueAddingByteProcessor.getResult() & FixConstants.CHECK_SUM_MODULO_MASK;
            FieldUtils.writeEncoded(checksum, out, CHECKSUM_VALUE_LENGTH);
            out.writeByte(FixMessage.FIELD_SEPARATOR);
        } finally {
            valueAddingByteProcessor.reset();
        }
    }
}
