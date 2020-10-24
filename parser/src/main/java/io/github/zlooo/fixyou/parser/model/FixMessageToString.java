package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;

@UtilityClass
class FixMessageToString {

    private static final char FIELD_DELIMITER = '|';
    private static final int LONG_MESSAGE_FIELD_NUMBER_THRESHOLD = 10;

    static String toString(FixMessage message, boolean wholeMessage) {
        final StringBuilder builder = new StringBuilder("FixMessage -> ");
        final Field[] fieldsOrdered = message.getFieldsOrdered();
        final boolean longMessage = fieldsOrdered.length > LONG_MESSAGE_FIELD_NUMBER_THRESHOLD;
        final boolean shortenOutput = longMessage && !wholeMessage;
        if (shortenOutput) {
            for (int i = 0; i < LONG_MESSAGE_FIELD_NUMBER_THRESHOLD; i++) {
                final Field field = fieldsOrdered[i];
                appendFieldToBuilderIfValueIsSet(builder, field);
            }
        } else {
            for (final Field field : fieldsOrdered) {
                appendFieldToBuilderIfValueIsSet(builder, field);
            }
        }
        builder.deleteCharAt(builder.length() - 1).append(shortenOutput ? "..." : "").append(", refCnt=").append(message.refCnt());
        return builder.toString();
    }

    private static void appendFieldToBuilderIfValueIsSet(StringBuilder builder, Field field) {
        if (field.isValueSet()) {
            builder.append(field.getNumber()).append((char) FixMessage.FIELD_VALUE_SEPARATOR).append(fieldDataValue(field)).append(FIELD_DELIMITER);
        }
    }

    private static String fieldDataValue(Field field) {
        final ByteBufComposer fieldData = field.getFieldData();
        if (fieldData != null) {
            final ByteBuf buf = Unpooled.buffer(field.getLength());
            fieldData.getBytes(field.getStartIndex(), field.getLength(), buf);
            return new String(buf.array(), StandardCharsets.US_ASCII);
        } else {
            return "";
        }
    }
}
