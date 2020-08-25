package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;

@UtilityClass
class FixMessageToString {

    private static final char FIELD_DELIMITER = '|';
    private static final int LONG_MESSAGE_FIELD_NUMBER_THRESHOLD = 10;

    static String toString(FixMessage message, boolean wholeMessage) {
        final StringBuilder builder = new StringBuilder("FixMessage -> ");
        final AbstractField[] fieldsOrdered = message.getFieldsOrdered();
        final boolean longMessage = fieldsOrdered.length > LONG_MESSAGE_FIELD_NUMBER_THRESHOLD;
        final boolean shortenOutput = longMessage && !wholeMessage;
        if (shortenOutput) {
            for (int i = 0; i < LONG_MESSAGE_FIELD_NUMBER_THRESHOLD; i++) {
                final AbstractField field = fieldsOrdered[i];
                appendFieldToBuilderIfValueIsSet(builder, field);
            }
        } else {
            for (final AbstractField field : fieldsOrdered) {
                appendFieldToBuilderIfValueIsSet(builder, field);
            }
        }
        builder.deleteCharAt(builder.length() - 1).append(shortenOutput ? "..." : "").append(", refCnt=").append(message.refCnt());
        return builder.toString();
    }

    private static void appendFieldToBuilderIfValueIsSet(StringBuilder builder, AbstractField field) {
        if (field.isValueSet()) {
            builder.append(field.number).append((char) FixMessage.FIELD_VALUE_SEPARATOR).append(fieldDataValue(field)).append(FIELD_DELIMITER);
        }
    }

    private static String fieldDataValue(AbstractField field) {
        final ByteBufComposer fieldData = field.fieldData;
        if (fieldData != null) {
            final byte[] buf = new byte[field.getLength()];
            fieldData.getBytes(field.startIndex, field.getLength(), buf);
            return new String(buf, StandardCharsets.US_ASCII);
        } else {
            return "";
        }
    }
}
