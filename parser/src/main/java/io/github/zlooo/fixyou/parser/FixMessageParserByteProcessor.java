package io.github.zlooo.fixyou.parser;

import com.carrotsearch.hppcrt.IntByteMap;
import com.carrotsearch.hppcrt.IntDeque;
import com.carrotsearch.hppcrt.IntObjectMap;
import com.carrotsearch.hppcrt.cursors.IntCursor;
import com.carrotsearch.hppcrt.cursors.ObjectCursor;
import com.carrotsearch.hppcrt.lists.IntArrayDeque;
import com.carrotsearch.hppcrt.procedures.IntProcedure;
import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.utils.DateUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.parser.model.FixMessageRepeatingGroupUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FixMessageParserByteProcessor implements ByteProcessor, Closeable, Resettable {
    private static final String FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG = "Field {} not found in message spec";
    private static final String UNSUPPORTED_FIELD_TYPE = "Unsupported field type ";
    private static final int CHECKSUM_WITH_SEPARATOR_LENGTH = 3;
    private static final int CHECKSUM_VALUE_LENGTH = 3;

    private final FixMessage fixMessage;
    private final ByteBuf composerBuffer = Unpooled.directBuffer();
    //parsing stuff
    private final IntObjectMap<FieldType> numberToFieldType;
    private final DateUtils.TimestampParser timestampParser = new DateUtils.TimestampParser();
    //TODO what a mess :/ try to think of a way to clean this up
    //repeating groups stuff
    private final IntObjectMap<IntByteMap> groupFieldsConstituents;
    //each element of this stack consists of 1 byte of repetition index followed by 3 bytes fo group number
    private final IntDeque groupIndexNumberStack = new IntArrayDeque(DefaultConfiguration.NESTED_REPEATING_GROUPS);
    private boolean parsingRepeatingGroup;
    //holders
    private final ValueHolders.IntHolder fieldNumberHolder = new ValueHolders.IntHolder(FixMessageParser.NO_VALUE);
    private final ValueHolders.IntHolder counter = new ValueHolders.IntHolder();
    private final ValueHolders.IntHolder fieldParsingCounter = new ValueHolders.IntHolder();
    private final ValueHolders.DecimalHolder decimalHolder = new ValueHolders.DecimalHolder();
    private final ValueHolders.LongHolder longHolder = new ValueHolders.LongHolder();

    private int bodyLengthEndIndex;

    @Override
    public boolean process(byte value) throws Exception {
        counter.getAndIncrement();
        switch (value) {
            case FixMessage.FIELD_VALUE_SEPARATOR:
                FieldValueParser.parseInteger(composerBuffer, fieldNumberHolder);
                composerBuffer.clear();
                break;
            case FixMessage.FIELD_SEPARATOR:
                final boolean proceedFurther = handleValue(composerBuffer);
                composerBuffer.clear();
                if (!proceedFurther) {
                    counter.setValue(0);
                }
                return proceedFurther;
            default:
                composerBuffer.writeByte(value);
        }
        return true;
    }

    private boolean handleValue(ByteBuf fieldValueBuffer) {
        final int fieldNumber = fieldNumberHolder.getValue();
        final boolean isChecksumField = FixConstants.CHECK_SUM_FIELD_NUMBER == fieldNumber;
        if (isChecksumField) {
            fixMessage.setBodyLength(counter.getValue() - bodyLengthEndIndex - CHECKSUM_WITH_SEPARATOR_LENGTH - CHECKSUM_VALUE_LENGTH - 1); //reader index is advanced by 7 too far because 10=XXX\u0001 has already been read
        }
        if (FixConstants.BODY_LENGTH_FIELD_NUMBER == fieldNumber) {
            bodyLengthEndIndex = counter.getValue();
        }
        final FieldType fieldType = numberToFieldType.get(fieldNumber);
        if (fieldType != null) {
            if (!parsingRepeatingGroup) {
                setFieldValue(fieldNumber, fieldType, fieldValueBuffer);
                if (FieldType.GROUP == fieldType) {
                    parsingRepeatingGroup = true;
                    groupIndexNumberStack.addFirst(fieldNumber);
                }
            } else {
                setRepeatingGroupFieldValue(fieldNumber, fieldType, fieldValueBuffer);
            }
        } else {
            log.debug(FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG, fieldNumber);
        }
        if (isChecksumField) {
            return false;
        }
        return true;
    }

    //TODO I don't feel it's neat solution, I'd better think of something better, but it's good enough for now
    private void setRepeatingGroupFieldValue(int parsedFieldNumber, FieldType fieldType, ByteBuf fieldValueBuffer) {
        int groupIndexNumber = groupIndexNumberStack.getFirst();
        int groupNumber = groupIndexNumber & FixMessageRepeatingGroupUtils.GROUP_NUMBER_MASK;
        final byte parentRepetitionIndex;
        final IntByteMap groupConstituents = groupFieldsConstituents.get(groupNumber);
        final byte newRepetitionIndex;
        if (groupConstituents.get(parsedFieldNumber) ==
            FixMessageParser.NO_VALUE) { //field is not part of current repeating group, it's either normal field(not part of repeating group) or part of parent repeating group(in case we've got nested group)
            groupIndexNumber = groupIndexNumber(parsedFieldNumber);
            groupNumber = groupIndexNumber & FixMessageRepeatingGroupUtils.GROUP_NUMBER_MASK;
            if (groupIndexNumber == FixMessageParser.NO_VALUE) { //normal field(not part of repeating group)
                setFieldValue(parsedFieldNumber, fieldType, fieldValueBuffer);
                return;
            } else {
                newRepetitionIndex = groupFieldsConstituents.get(groupIndexNumber).addTo(parsedFieldNumber, (byte) 1);
                parentRepetitionIndex = parentRepetitionIndex();
            }
        } else {
            newRepetitionIndex = groupConstituents.addTo(parsedFieldNumber, (byte) 1);
            parentRepetitionIndex = parentRepetitionIndex();
        }
        if (FieldType.GROUP == fieldType) {
            groupIndexNumberStack.addFirst(FixMessageRepeatingGroupUtils.groupIndex(newRepetitionIndex, parsedFieldNumber));
        }
        setFieldValue(parsedFieldNumber, groupNumber, newRepetitionIndex, parentRepetitionIndex, fieldType, fieldValueBuffer);
    }

    private byte parentRepetitionIndex() {
        if (!groupIndexNumberStack.isEmpty()) {
            return (byte) (groupIndexNumberStack.getFirst() >> FixMessageRepeatingGroupUtils.REPEATING_GROUP_NUMBER_KEY_SHIFT_NUMBER);
        } else {
            return 0;
        }
    }

    private int groupIndexNumber(int fieldNum) {
        while (!groupIndexNumberStack.isEmpty()) {
            final int groupNumber = groupIndexNumberStack.getFirst() & FixMessageRepeatingGroupUtils.GROUP_NUMBER_MASK;
            final IntByteMap groupConstituents = groupFieldsConstituents.get(groupNumber);
            if (groupConstituents.containsKey(fieldNum)) {
                return groupNumber;
            } else {
                groupIndexNumberStack.removeFirst();
                groupConstituents.keys().forEach((IntProcedure) key -> groupConstituents.put(key, FixMessageParser.GROUP_CONSTITUENTS_INITIAL_VALUE));
            }
        }
        parsingRepeatingGroup = false;
        return FixMessageParser.NO_VALUE;
    }

    private void setFieldValue(int fieldNumber, FieldType fieldType, ByteBuf fieldValueBuffer) {
        switch (fieldType) {
            case BOOLEAN:
                fixMessage.setBooleanValue(fieldNumber, FieldValueParser.parseBoolean(fieldValueBuffer));
                break;
            case CHAR:
                fixMessage.setCharValue(fieldNumber, FieldValueParser.parseChar(fieldValueBuffer));
                break;
            case CHAR_ARRAY:
                fixMessage.setCharSequenceValue(fieldNumber, fieldValueBuffer);
                break;
            case DOUBLE:
                FieldValueParser.setDoubleValuesFromAsciiByteBuf(fieldValueBuffer, fieldParsingCounter, decimalHolder);
                fixMessage.setDoubleValue(fieldNumber, decimalHolder.getUnscaledValue(), decimalHolder.getScale());
                break;
            case LONG:
            case GROUP:
                fixMessage.setLongValue(fieldNumber, FieldValueParser.parseLong(fieldValueBuffer, longHolder));
                break;
            case TIMESTAMP:
                timestampParser.reset();
                fixMessage.setTimestampValue(fieldNumber, DateUtils.parseTimestamp(fieldValueBuffer, timestampParser));
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_FIELD_TYPE + fieldType);
        }
    }

    private void setFieldValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentGroupRepetitionIndex, FieldType fieldType, ByteBuf fieldValueBuffer) {
        switch (fieldType) {
            case BOOLEAN:
                fixMessage.setBooleanValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, FieldValueParser.parseBoolean(fieldValueBuffer));
                break;
            case CHAR:
                fixMessage.setCharValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, FieldValueParser.parseChar(fieldValueBuffer));
                break;
            case CHAR_ARRAY:
                fixMessage.setCharSequenceValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, fieldValueBuffer);
                break;
            case DOUBLE:
                FieldValueParser.setDoubleValuesFromAsciiByteBuf(fieldValueBuffer, fieldParsingCounter, decimalHolder);
                fixMessage.setDoubleValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, decimalHolder.getUnscaledValue(), decimalHolder.getScale());
                break;
            case LONG:
            case GROUP:
                fixMessage.setLongValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, FieldValueParser.parseLong(fieldValueBuffer, longHolder));
                break;
            case TIMESTAMP:
                timestampParser.reset();
                fixMessage.setTimestampValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, DateUtils.parseTimestamp(fieldValueBuffer, timestampParser));
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_FIELD_TYPE + fieldType);
        }
    }

    @Override
    public void close() {
        composerBuffer.release();
    }

    @Override
    public void reset() {
        parsingRepeatingGroup = false;
        groupIndexNumberStack.clear();
        for (final ObjectCursor<IntByteMap> constituentsCursor : groupFieldsConstituents.values()) {
            for (final IntCursor groupFieldsCursor : constituentsCursor.value.keys()) {
                constituentsCursor.value.put(groupFieldsCursor.value, FixMessageParser.GROUP_CONSTITUENTS_INITIAL_VALUE);
            }
        }
        bodyLengthEndIndex = 0;
    }
}
