package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.netty.utils.ValueAddingByteProcessor;
import io.github.zlooo.fixyou.parser.model.AbstractField;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
abstract class AbstractMessageEncoder extends MessageToByteEncoder<FixMessage> {

    public static final int CHECKSUM_VALUE_LENGTH = 3;

    @Override
    protected void encode(ChannelHandlerContext ctx, FixMessage msg, ByteBuf out) {
        final AbstractField[] fieldsOrdered = msg.getFieldsOrdered();
        out.writeBytes(fieldsOrdered[0].getEncodedFieldNumber()).writeBytes(fieldsOrdered[0].getFieldData()).writeByte(FixMessage.FIELD_SEPARATOR);
        out.writeBytes(fieldsOrdered[1].getEncodedFieldNumber());
        final ByteBuf bodyTempBuffer = getBodyTempBuffer();
        for (int i = 2; i < fieldsOrdered.length - 1; i++) {
            final AbstractField field = fieldsOrdered[i];
            if (field.isValueSet()) {
                //TODO think how it can be done without temp buffer, it is faster to first count number of bytes to fill body length?
                final ByteBuf fieldData = field.getFieldData();
                fieldData.readerIndex(0);
                bodyTempBuffer.writeBytes(field.getEncodedFieldNumber()).writeBytes(fieldData).writeByte(FixMessage.FIELD_SEPARATOR);
            }
        }
        appendBodyLength(out, bodyTempBuffer.writerIndex());
        out.writeBytes(bodyTempBuffer);
        bodyTempBufferNotNeeded(bodyTempBuffer);
        appendWithChecksum(out, msg.getField(FixConstants.CHECK_SUM_FIELD_NUMBER), getValueAddingByteProcessor());
        if (log.isDebugEnabled()) {
            log.debug("Encoded message " + out.toString(StandardCharsets.US_ASCII));
        }
    }

    protected abstract ValueAddingByteProcessor getValueAddingByteProcessor();

    protected abstract ByteBuf getBodyTempBuffer();

    protected void bodyTempBufferNotNeeded(ByteBuf bodyTempBuffer) {
        //nothing to do
    }

    private static void appendWithChecksum(ByteBuf out, AbstractField checksumField, ValueAddingByteProcessor valueAddingByteProcessor) {
        try {
            out.forEachByte(valueAddingByteProcessor);
            out.writeBytes(checksumField.getEncodedFieldNumber());
            final int checksum = valueAddingByteProcessor.getResult() & FixConstants.CHECK_SUM_MODULO_MASK;
            FieldUtils.writeEncoded(checksum, out, CHECKSUM_VALUE_LENGTH);
            out.writeByte(FixMessage.FIELD_SEPARATOR);
        } finally {
            valueAddingByteProcessor.reset();
        }
    }

    private static void appendBodyLength(ByteBuf out, int restOfBodyLength) {
        FieldUtils.writeEncoded(restOfBodyLength, out);
        out.writeByte(FixMessage.FIELD_SEPARATOR);
    }
}
