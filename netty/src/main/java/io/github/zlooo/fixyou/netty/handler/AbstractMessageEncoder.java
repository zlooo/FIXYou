package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.ReusableCharArray;
import io.github.zlooo.fixyou.commons.utils.DateUtils;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.commons.utils.NumberConstants;
import io.github.zlooo.fixyou.commons.utils.ReflectionUtils;
import io.github.zlooo.fixyou.model.DefaultExtendedFixSpec;
import io.github.zlooo.fixyou.model.ExtendedFixSpec;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.netty.utils.FixSpec50SP2;
import io.github.zlooo.fixyou.parser.FieldValueParser;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AsciiString;
import lombok.AccessLevel;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
@FieldNameConstants(level = AccessLevel.PRIVATE)
abstract class AbstractMessageEncoder extends MessageToByteEncoder<FixMessage> {

    private static final int SINGLE_DIGIT_TAG_LENGTH = 2; //length of single digit tags, for example 8=
    private static final int DOUBLE_DIGIT_TAG_LENGTH = 3; //length of single digit tags, for example 35=
    private static final int CHECKSUM_VALUE_LENGTH = 3;
    private static final int MAX_BODY_LENGTH_FIELD_LENGTH = 8; //on one hand we should -1 it because it can only be positive value but on the other we have to terminate this field with SOH so it evens out
    private static final int INITIAL_BYTE_BUFFER_LENGTH = 32;
    private static final ExtendedFixSpec BACKUP_SPEC = new DefaultExtendedFixSpec(new FixSpec50SP2());
    private static final double SCALE_FACTOR = 1.3;
    private static final String UNSUPPORTED_FIELD_TYPE_ERROR_MSG_TEMPLATE = "Unsupported field type %s, field number %d";
    private static final int MANDATORY_HEADER_FIELDS = 3;

    private final byte[] tempBuffer = new byte[INITIAL_BYTE_BUFFER_LENGTH];

    @Override
    protected void encode(ChannelHandlerContext ctx, FixMessage msg, ByteBuf out) {
        final char beginStringLength = msg.getCharSequenceLength(FixConstants.BEGIN_STRING_FIELD_NUMBER);
        final int afterMsgTypeIndex = SINGLE_DIGIT_TAG_LENGTH + beginStringLength + SINGLE_DIGIT_TAG_LENGTH + MAX_BODY_LENGTH_FIELD_LENGTH + 2;
        final char messageTypeLength = msg.getCharSequenceLength(FixConstants.MESSAGE_TYPE_FIELD_NUMBER);
        out.writerIndex(afterMsgTypeIndex + DOUBLE_DIGIT_TAG_LENGTH + messageTypeLength + 1);
        final NettyHandlerAwareSessionState sessionState = NettyHandlerAwareSessionState.getForChannelContext(ctx);
        final ExtendedFixSpec fixSpec = sessionState != null ? sessionState.getFixSpec() : BACKUP_SPEC; //it may happen that session is not yet established at this point, for example reject as a response to logon message
        int sumOfBytes = writeHeaderFields(msg, out, fixSpec);
        sumOfBytes += writeFields(msg, out, fixSpec);
        sumOfBytes += prependThreeFirstFields(out, afterMsgTypeIndex, msg, beginStringLength);
        appendWithChecksum(out, sumOfBytes);
        if (log.isDebugEnabled()) {
            log.debug("Encoded message " + out.toString(StandardCharsets.US_ASCII) + " buffer " + out);
        }
    }

    protected int writeHeaderFields(FixMessage msg, ByteBuf out, ExtendedFixSpec fixSpec) {
        final int[] headerFields = fixSpec.getHeaderFieldsOrder();
        final FieldType[] headerFieldsType = fixSpec.getHeaderFieldTypes();
        int sumOfBytes = 0;
        for (int i = MANDATORY_HEADER_FIELDS; i < headerFields.length; i++) {
            final int fieldNumber = ArrayUtils.getElementAt(headerFields, i);
            if (msg.isValueSet(fieldNumber)) {
                final FieldType fieldType = ArrayUtils.getElementAt(headerFieldsType, i);
                //TODO check if bulk move, if 1 out.writeBytes per field, will perform better
                sumOfBytes += writeTagAndSeparator(fieldNumber, out);
                sumOfBytes += writeValue(msg, fixSpec, fieldNumber, fieldType, out);
                out.writeByte(FixMessage.FIELD_SEPARATOR);
                sumOfBytes += FixMessage.FIELD_SEPARATOR;
            }
        }
        return sumOfBytes;
    }

    protected abstract int writeFields(FixMessage msg, ByteBuf out, ExtendedFixSpec fixSpec);

    protected static int writeTagAndSeparator(int fieldNumber, ByteBuf out) {
        int sumOfBytes = FieldUtils.writeEncoded(fieldNumber, out);
        out.writeByte(FixMessage.FIELD_VALUE_SEPARATOR);
        sumOfBytes += FixMessage.FIELD_VALUE_SEPARATOR;
        return sumOfBytes;
    }

    protected int writeValue(FixMessage msg, FixSpec fixSpec, int fieldNumber, FieldType fieldType, ByteBuf out) {
        final int sumOfWrittenBytes;
        switch (fieldType) {
            case BOOLEAN:
                sumOfWrittenBytes = writeBooleanValue(msg.getBooleanValue(fieldNumber), out);
                break;
            case CHAR:
                sumOfWrittenBytes = writeCharValue(msg.getCharValue(fieldNumber), out);
                break;
            case CHAR_ARRAY:
                sumOfWrittenBytes = writeCharSequenceValue(msg.getCharSequenceValue(fieldNumber), out);
                break;
            case DOUBLE:
                sumOfWrittenBytes = writeDoubleValue(msg.getDoubleUnscaledValue(fieldNumber), msg.getScale(fieldNumber), out);
                break;
            case LONG:
                sumOfWrittenBytes = FieldUtils.writeEncoded(msg.getLongValue(fieldNumber), out);
                break;
            case TIMESTAMP:
                sumOfWrittenBytes = DateUtils.writeTimestamp(msg.getTimestampValue(fieldNumber), out, true);
                break;
            case GROUP:
                sumOfWrittenBytes = writeRepeatingGroup(msg, fixSpec, fieldNumber, (byte) 0, out);
                break;
            default:
                throw new IllegalArgumentException(String.format(UNSUPPORTED_FIELD_TYPE_ERROR_MSG_TEMPLATE, fieldType, fieldNumber));
        }
        return sumOfWrittenBytes;
    }

    private int writeRepeatingGroup(FixMessage msg, FixSpec fixSpec, int groupNumber, byte parentRepetitionIndex, ByteBuf out) {
        final long groupRepetitionNumber = msg.getLongValue(groupNumber);
        int sumOfBytes = FieldUtils.writeEncoded(groupRepetitionNumber, out);
        out.writeByte(FixMessage.FIELD_SEPARATOR);
        sumOfBytes += FixMessage.FIELD_SEPARATOR;
        final FixSpec.FieldNumberType[] repeatingGroupFieldNumbers = fixSpec.getRepeatingGroupFieldNumbers(groupNumber);
        for (byte repetitionIndex = 0; repetitionIndex < groupRepetitionNumber; repetitionIndex++) {
            sumOfBytes += writeGroupSingleRepetition(msg, fixSpec, groupNumber, parentRepetitionIndex, out, repeatingGroupFieldNumbers, repetitionIndex);
        }
        out.writerIndex(out.writerIndex() - 1);
        return sumOfBytes - 1;
    }

    private int writeGroupSingleRepetition(FixMessage msg, FixSpec fixSpec, int groupNumber, byte parentRepetitionIndex, ByteBuf out, FixSpec.FieldNumberType[] repeatingGroupFieldNumbers, byte repetitionIndex) {
        int sumOfBytes = 0;
        for (int repGroupFieldIndex = 0; repGroupFieldIndex < repeatingGroupFieldNumbers.length; repGroupFieldIndex++) {
            final FixSpec.FieldNumberType fieldNumberType = ArrayUtils.getElementAt(repeatingGroupFieldNumbers, repGroupFieldIndex);
            final int repeatingGroupConstituent = fieldNumberType.getNumber();
            final FieldType fieldType = fieldNumberType.getType();
            if (isRepeatingGroupValueSet(msg, fieldType, groupNumber, parentRepetitionIndex, repetitionIndex, repeatingGroupConstituent)) {
                sumOfBytes += writeTagAndSeparator(repeatingGroupConstituent, out);
                switch (fieldType) {
                    case BOOLEAN:
                        sumOfBytes += writeBooleanValue(msg.getBooleanValue(repeatingGroupConstituent, groupNumber, repetitionIndex, parentRepetitionIndex), out);
                        break;
                    case CHAR:
                        sumOfBytes += writeCharValue(msg.getCharValue(repeatingGroupConstituent, groupNumber, repetitionIndex, parentRepetitionIndex), out);
                        break;
                    case CHAR_ARRAY:
                        sumOfBytes += writeCharSequenceValue(msg.getCharSequenceValue(repeatingGroupConstituent, groupNumber, repetitionIndex, parentRepetitionIndex), out);
                        break;
                    case DOUBLE:
                        sumOfBytes += writeDoubleValue(msg.getDoubleUnscaledValue(repeatingGroupConstituent, groupNumber, repetitionIndex, parentRepetitionIndex),
                                                       msg.getScale(repeatingGroupConstituent, groupNumber, repetitionIndex, parentRepetitionIndex), out);
                        break;
                    case LONG:
                        sumOfBytes += FieldUtils.writeEncoded(msg.getLongValue(repeatingGroupConstituent, groupNumber, repetitionIndex, parentRepetitionIndex), out);
                        break;
                    case TIMESTAMP:
                        sumOfBytes += DateUtils.writeTimestamp(msg.getTimestampValue(repeatingGroupConstituent, groupNumber, repetitionIndex, parentRepetitionIndex), out, true);
                        break;
                    case GROUP:
                        sumOfBytes += writeRepeatingGroup(msg, fixSpec, repeatingGroupConstituent, repetitionIndex, out);
                        break;
                    default:
                        throw new IllegalArgumentException(String.format(UNSUPPORTED_FIELD_TYPE_ERROR_MSG_TEMPLATE, fieldType, repeatingGroupConstituent));
                }
                out.writeByte(FixMessage.FIELD_SEPARATOR);
                sumOfBytes += FixMessage.FIELD_SEPARATOR;
            }
        }
        return sumOfBytes;
    }

    private boolean isRepeatingGroupValueSet(FixMessage msg, FieldType fieldType, int groupNumber, byte parentRepetitionIndex, byte index, int repeatingGroupConstituent) {
        if (FieldType.GROUP == fieldType) {
            return msg.isValueSet(repeatingGroupConstituent);
        } else {
            return msg.isValueSet(repeatingGroupConstituent, groupNumber, index, parentRepetitionIndex);
        }
    }

    private int writeDoubleValue(long doubleUnscaledValue, short scale, ByteBuf out) {
        //TODO refactor this so that value is written directly, not converted to char[] first
        final ReusableCharArray valueAsChar = FieldUtils.toCharSequence(doubleUnscaledValue, 1);
        final int length = valueAsChar.length();
        ensureTempBuffersCapacity(length);
        final int separatorIndex = length - scale - 1;
        final char[] valueAsCharArray = valueAsChar.getCharArray();
        ArrayUtils.insertElementAtIndex(valueAsCharArray, FieldValueParser.FRACTION_SEPARATOR, separatorIndex);
        int sumOfBytes = 0;
        for (int i = 0; i < length; i++) {
            final byte byteToWrite = AsciiString.c2b(ArrayUtils.getElementAt(valueAsCharArray, i));
            sumOfBytes += byteToWrite;
            ArrayUtils.putElementAt(tempBuffer, i, byteToWrite);
        }
        out.writeBytes(tempBuffer, 0, length);
        return sumOfBytes;
    }

    private int writeCharSequenceValue(CharSequence charSequenceValue, ByteBuf out) {
        int sumOfBytes = 0;
        final int charSequenceLength = charSequenceValue.length();
        ensureTempBuffersCapacity(charSequenceLength);
        for (int i = 0; i < charSequenceLength; i++) {
            final byte encodedChar = AsciiString.c2b(charSequenceValue.charAt(i));
            sumOfBytes += encodedChar;
            ArrayUtils.putElementAt(tempBuffer, i, encodedChar);
        }
        out.writeBytes(tempBuffer, 0, charSequenceLength);
        return sumOfBytes;
    }

    private void ensureTempBuffersCapacity(int length) {
        if (tempBuffer.length < length) {
            ReflectionUtils.setFinalField(this, Fields.tempBuffer, new byte[(int) (length * SCALE_FACTOR)]);
        }
    }

    private static int writeCharValue(char charValue, ByteBuf out) {
        final int byteToWrite = AsciiString.c2b(charValue);
        out.writeByte(byteToWrite);
        return byteToWrite;
    }

    private static int writeBooleanValue(boolean booleanValue, ByteBuf out) {
        if (booleanValue) {
            out.writeByte(AsciiCodes.Y);
            return AsciiCodes.Y;
        } else {
            out.writeByte(AsciiCodes.N);
            return AsciiCodes.N;
        }
    }

    private int prependThreeFirstFields(ByteBuf out, int afterBodyLengthIndex, FixMessage fixMessage, char beginStringLength) {
        final int bodyLengthValue = out.writerIndex() - afterBodyLengthIndex;
        int powerOfTenIndex = 0;
        for (; powerOfTenIndex < NumberConstants.POWERS_OF_TEN.length; powerOfTenIndex++) {
            if (NumberConstants.POWERS_OF_TEN[powerOfTenIndex] > bodyLengthValue) {
                break;
            }
        }
        final int startingIndex = afterBodyLengthIndex - SINGLE_DIGIT_TAG_LENGTH - beginStringLength - SINGLE_DIGIT_TAG_LENGTH - powerOfTenIndex - 2;
        out.markWriterIndex();
        out.readerIndex(startingIndex).writerIndex(startingIndex);
        int sumOfBytes = writeTagAndSeparator(FixConstants.BEGIN_STRING_FIELD_NUMBER, out);
        sumOfBytes += writeCharSequenceValue(fixMessage.getCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER), out) + FixMessage.FIELD_SEPARATOR;
        out.writeByte(FixMessage.FIELD_SEPARATOR);
        sumOfBytes += writeTagAndSeparator(FixConstants.BODY_LENGTH_FIELD_NUMBER, out);
        sumOfBytes += FieldUtils.writeEncoded(bodyLengthValue, out) + FixMessage.FIELD_SEPARATOR;
        out.writeByte(FixMessage.FIELD_SEPARATOR);
        sumOfBytes += writeTagAndSeparator(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, out);
        sumOfBytes += writeCharSequenceValue(fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER), out) + FixMessage.FIELD_SEPARATOR;
        out.writeByte(FixMessage.FIELD_SEPARATOR).resetWriterIndex();
        return sumOfBytes;
    }

    private static void appendWithChecksum(ByteBuf out, int sumOfBytes) {
        writeTagAndSeparator(FixConstants.CHECK_SUM_FIELD_NUMBER, out);
        final int checksum = sumOfBytes & FixConstants.CHECK_SUM_MODULO_MASK;
        FieldUtils.writeEncoded(checksum, out, CHECKSUM_VALUE_LENGTH);
        out.writeByte(FixMessage.FIELD_SEPARATOR);
    }

    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, FixMessage msg, boolean preferDirect) throws Exception {
        return ctx.alloc().ioBuffer(DefaultConfiguration.DEFAULT_OUT_MESSAGE_BUF_INIT_CAPACITY);
    }
}
