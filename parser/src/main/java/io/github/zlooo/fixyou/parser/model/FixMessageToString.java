package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.util.ReferenceCountUtil;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
class FixMessageToString {

    private static final char FIELD_DELIMITER = '|';
    private static final int LONG_MESSAGE_FIELD_NUMBER_THRESHOLD = 10;

    static String toString(FixMessage message, boolean wholeMessage, FixSpec fixSpec) {
        final StringBuilder builder = new StringBuilder("FixMessage -> [");
        final List<FixSpec.FieldNumberType> fieldNumberTypes = new ArrayList<>();
        addFieldNumberTypes(message, fixSpec.getHeaderFieldsOrder(), fixSpec.getHeaderFieldTypes(), fieldNumberTypes);
        addFieldNumberTypes(message, fixSpec.getBodyFieldsOrder(), fixSpec.getBodyFieldTypes(), fieldNumberTypes);
        final int fieldsWithValueLength = fieldNumberTypes.size();
        final boolean longMessage = fieldsWithValueLength > LONG_MESSAGE_FIELD_NUMBER_THRESHOLD;
        final boolean shortenOutput = longMessage && !wholeMessage;
        final int fieldsLimit = shortenOutput ? LONG_MESSAGE_FIELD_NUMBER_THRESHOLD : fieldsWithValueLength;
        for (int i = 0; i < fieldsLimit; i++) {
            appendFieldToBuilder(builder, message, fieldNumberTypes.get(i));
        }
        builder.deleteCharAt(builder.length() - 1).append(shortenOutput ? "..." : "").append("], refCnt=").append(ReferenceCountUtil.refCnt(message));
        return builder.toString();
    }

    private static void addFieldNumberTypes(FixMessage message, int[] fieldsOrder, FieldType[] fieldTypes, List<FixSpec.FieldNumberType> fieldNumberTypes) {
        for (int i = 0; i < fieldsOrder.length; i++) {
            final int fieldNumber = ArrayUtils.getElementAt(fieldsOrder, i);
            if (message.isValueSet(fieldNumber)) {
                fieldNumberTypes.add(new FixSpec.FieldNumberType(fieldNumber, ArrayUtils.getElementAt(fieldTypes, i)));
            }
        }
    }

    private static void appendFieldToBuilder(StringBuilder builder, FixMessage message, FixSpec.FieldNumberType fieldNumberType) {
        final int fieldNumber = fieldNumberType.getNumber();
        builder.append(fieldNumber).append("=");
        switch (fieldNumberType.getType()) {
            case BOOLEAN:
                builder.append(message.getBooleanValue(fieldNumber) ? "Y" : "N");
                break;
            case CHAR:
                builder.append(message.getCharValue(fieldNumber));
                break;
            case CHAR_ARRAY:
                builder.append(message.getCharSequenceValue(fieldNumber));
                break;
            case DOUBLE:
                builder.append(new BigDecimal(BigInteger.valueOf(message.getDoubleUnscaledValue(fieldNumber)), message.getScale(fieldNumber)));
                break;
            case LONG:
                builder.append(message.getLongValue(fieldNumber));
                break;
            case GROUP:
                builder.append(message.getLongValue(fieldNumber));
                //TODO I know toString does not print out repeating group content but it's not very important at the moment
                break;
            case TIMESTAMP:
                builder.append(message.getTimestampValue(fieldNumber));
                break;
            default:
                throw new IllegalArgumentException("Unsupported message type " + fieldNumberType.getType());
        }
        builder.append(FIELD_DELIMITER);
    }
}
