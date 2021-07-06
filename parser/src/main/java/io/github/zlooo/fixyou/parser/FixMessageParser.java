package io.github.zlooo.fixyou.parser;

import com.carrotsearch.hppcrt.IntByteMap;
import com.carrotsearch.hppcrt.IntDeque;
import com.carrotsearch.hppcrt.IntObjectMap;
import com.carrotsearch.hppcrt.cursors.IntCursor;
import com.carrotsearch.hppcrt.cursors.ObjectCursor;
import com.carrotsearch.hppcrt.lists.IntArrayDeque;
import com.carrotsearch.hppcrt.maps.IntByteHashMap;
import com.carrotsearch.hppcrt.maps.IntObjectHashMap;
import com.carrotsearch.hppcrt.procedures.IntProcedure;
import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.commons.utils.DateUtils;
import io.github.zlooo.fixyou.commons.utils.ReflectionUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.model.FixMessageRepeatingGroupUtils;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class FixMessageParser implements Resettable {

    private static final String FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG = "Field {} not found in message spec";
    private static final String UNSUPPORTED_FIELD_TYPE = "Unsupported field type ";
    private static final byte GROUP_CONSTITUENTS_INITIAL_VALUE = (byte) -1;
    private static final byte NO_VALUE = (byte) -2;
    private static final int CHECKSUM_WITH_SEPARATOR_LENGTH = 3;

    @Getter
    private final ByteBufComposer bytesToParse;
    @Getter
    private final FixMessage fixMessage;
    //TODO what a mess :/ try to think of a way to clean this up
    //repeating groups stuff
    private final IntObjectMap<IntByteMap> groupFieldsConstituents = new IntObjectHashMap<>();
    //each element of this stack consists of 1 byte of repetition index followed by 3 bytes fo group number
    private final IntDeque groupIndexNumberStack = new IntArrayDeque(DefaultConfiguration.NESTED_REPEATING_GROUPS);
    private boolean parsingRepeatingGroup;
    //parsing stuff
    private final ByteBuf tempBuffer = Unpooled.directBuffer();
    private final char[] tempCharBuffer = new char[tempBuffer.capacity()];
    private final IntObjectMap<FieldType> numberToFieldType;
    private final DateUtils.TimestampParser timestampParser = new DateUtils.TimestampParser();
    //fragmentation handing stuff
    private int storedEndIndexOfLastUnfinishedMessage;
    private int bodyLengthEndIndex;

    public FixMessageParser(ByteBufComposer bytesToParse, FixSpec fixSpec, FixMessage fixMessage) {
        this.bytesToParse = bytesToParse;
        this.fixMessage = fixMessage;
        final int[] bodyFieldsOrder = fixSpec.getBodyFieldsOrder();
        final int numberOfBodyFields = bodyFieldsOrder.length;
        final int[] headerFieldsOrder = fixSpec.getHeaderFieldsOrder();
        final FieldType[] headerFieldTypes = fixSpec.getHeaderFieldTypes();
        final int numberOfHeaderFields = headerFieldsOrder.length;
        this.numberToFieldType = new IntObjectHashMap<>(numberOfBodyFields + numberOfHeaderFields);
        for (int i = 0; i < numberOfHeaderFields; i++) {
            numberToFieldType.put(ArrayUtils.getElementAt(headerFieldsOrder, i), ArrayUtils.getElementAt(headerFieldTypes, i));
        }
        final FieldType[] fieldTypes = fixSpec.getBodyFieldTypes();
        for (int i = 0; i < numberOfBodyFields; i++) {
            final int fieldNumber = ArrayUtils.getElementAt(bodyFieldsOrder, i);
            numberToFieldType.put(fieldNumber, ArrayUtils.getElementAt(fieldTypes, i));
            final FixSpec.FieldNumberType[] groupConstituents = fixSpec.getRepeatingGroupFieldNumbers(fieldNumber);
            final int repeatingGroupSize = groupConstituents.length;
            if (repeatingGroupSize > 0) {
                final IntByteMap groupFieldConstituents = new IntByteHashMap(repeatingGroupSize);
                groupFieldConstituents.setDefaultValue(NO_VALUE);
                for (int j = 0; j < repeatingGroupSize; j++) {
                    final FixSpec.FieldNumberType fieldNumberType = ArrayUtils.getElementAt(groupConstituents, j);
                    final int groupConstituentNumber = fieldNumberType.getNumber();
                    groupFieldConstituents.put(groupConstituentNumber, GROUP_CONSTITUENTS_INITIAL_VALUE);
                    numberToFieldType.put(groupConstituentNumber, fieldNumberType.getType());
                }
                this.groupFieldsConstituents.put(fieldNumber, groupFieldConstituents);
            }
        }
    }

    @Override
    public void reset() {
        fixMessage.reset();
        parsingRepeatingGroup = false;
        groupIndexNumberStack.clear();
        for (final ObjectCursor<IntByteMap> constituentsCursor : groupFieldsConstituents.values()) {
            for (final IntCursor groupFieldsCursor : constituentsCursor.value.keys()) {
                constituentsCursor.value.put(groupFieldsCursor.value, GROUP_CONSTITUENTS_INITIAL_VALUE);
            }
        }
        storedEndIndexOfLastUnfinishedMessage = 0;
        bodyLengthEndIndex = 0;
    }

    public void startParsing() {
        storedEndIndexOfLastUnfinishedMessage = 0;
    }

    public void parseFixMsgBytes() {
        int closestFieldTerminatorIndex;

        while (!bytesToParse.readerIndexBeyondStoredEnd() && ((closestFieldTerminatorIndex = bytesToParse.indexOfClosestSOH()) != ByteBufComposer.NOT_FOUND)) {
            final int parsedFieldNumber = FieldValueParser.parseInteger(bytesToParse, bytesToParse.readerIndex(), io.github.zlooo.fixyou.model.FixMessage.FIELD_VALUE_SEPARATOR, true);
            final int start = bytesToParse.readerIndex();
            final boolean isChecksumField = FixConstants.CHECK_SUM_FIELD_NUMBER == parsedFieldNumber;
            if (isChecksumField) {
                fixMessage.setBodyLength(start - bodyLengthEndIndex - CHECKSUM_WITH_SEPARATOR_LENGTH); //reader index is advanced by 3 too far because 10= has already been read
            }
            bytesToParse.readerIndex(closestFieldTerminatorIndex + 1); //end index
            if (FixConstants.BODY_LENGTH_FIELD_NUMBER == parsedFieldNumber) {
                bodyLengthEndIndex = bytesToParse.readerIndex();
            }
            final int length = bytesToParse.readerIndex() - start - 1;
            final FieldType fieldType = numberToFieldType.get(parsedFieldNumber);
            if (fieldType != null) {
                if (!parsingRepeatingGroup) {
                    setFieldValue(parsedFieldNumber, fieldType, start, length);
                    if (FieldType.GROUP == fieldType) {
                        parsingRepeatingGroup = true;
                        groupIndexNumberStack.addFirst(parsedFieldNumber);
                    }
                } else {
                    setRepeatingGroupFieldValue(parsedFieldNumber, start, length, fieldType);
                }
            } else {
                log.debug(FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG, parsedFieldNumber);
            }
            if (isChecksumField) {
                storedEndIndexOfLastUnfinishedMessage = 0;
                return;
            }
        }

        storedEndIndexOfLastUnfinishedMessage = bytesToParse.getStoredEndIndex();
    }

    //TODO I don't feel it's neat solution, I'd better think of something better, but it's good enough for now
    private void setRepeatingGroupFieldValue(int parsedFieldNumber, int start, int length, FieldType fieldType) {
        int groupIndexNumber = groupIndexNumberStack.getFirst();
        int groupNumber = groupIndexNumber & FixMessageRepeatingGroupUtils.GROUP_NUMBER_MASK;
        final byte parentRepetitionIndex;
        final IntByteMap groupConstituents = groupFieldsConstituents.get(groupNumber);
        final byte newRepetitionIndex;
        if (groupConstituents.get(parsedFieldNumber) == NO_VALUE) { //field is not part of current repeating group, it's either normal field(not part of repeating group) or part of parent repeating group(in case we've got nested group)
            groupIndexNumber = groupIndexNumber(parsedFieldNumber);
            groupNumber = groupIndexNumber & FixMessageRepeatingGroupUtils.GROUP_NUMBER_MASK;
            if (groupIndexNumber == NO_VALUE) { //normal field(not part of repeating group)
                setFieldValue(parsedFieldNumber, fieldType, start, length);
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
        setFieldValue(parsedFieldNumber, groupNumber, newRepetitionIndex, parentRepetitionIndex, fieldType, start, length);
    }

    public boolean canContinueParsing() {
        return !bytesToParse.readerIndexBeyondStoredEnd() && (storedEndIndexOfLastUnfinishedMessage == 0 || storedEndIndexOfLastUnfinishedMessage < bytesToParse.getStoredEndIndex());
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
                groupConstituents.keys().forEach((IntProcedure) key -> groupConstituents.put(key, GROUP_CONSTITUENTS_INITIAL_VALUE));
            }
        }
        parsingRepeatingGroup = false;
        return NO_VALUE;
    }

    private void setFieldValue(int fieldNumber, FieldType fieldType, int startIndex, int length) {
        switch (fieldType) {
            case BOOLEAN:
                fixMessage.setBooleanValue(fieldNumber, FieldValueParser.parseBoolean(bytesToParse, startIndex));
                break;
            case CHAR:
                fixMessage.setCharValue(fieldNumber, FieldValueParser.parseChar(bytesToParse, startIndex));
                break;
            case CHAR_ARRAY:
                checkTmpCharBuffer(length);
                FieldValueParser.readChars(bytesToParse, startIndex, length, tempBuffer, tempCharBuffer);
                fixMessage.setCharSequenceValue(fieldNumber, tempCharBuffer, length);
                break;
            case DOUBLE:
                checkTmpCharBuffer(length);
                FieldValueParser.setDoubleValuesFromByteBufComposer(bytesToParse, startIndex, length, tempBuffer, tempCharBuffer, (unscaledValue, scale) -> fixMessage.setDoubleValue(fieldNumber, unscaledValue, scale));
                break;
            case LONG:
            case GROUP:
                fixMessage.setLongValue(fieldNumber, FieldValueParser.parseLong(bytesToParse, startIndex, io.github.zlooo.fixyou.model.FixMessage.FIELD_SEPARATOR));
                break;
            case TIMESTAMP:
                timestampParser.reset();
                fixMessage.setTimestampValue(fieldNumber, DateUtils.parseTimestamp(bytesToParse, startIndex, length, timestampParser));
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_FIELD_TYPE + fieldType);
        }
    }

    private void setFieldValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentGroupRepetitionIndex, FieldType fieldType, int startIndex, int length) {
        switch (fieldType) {
            case BOOLEAN:
                fixMessage.setBooleanValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, FieldValueParser.parseBoolean(bytesToParse, startIndex));
                break;
            case CHAR:
                fixMessage.setCharValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, FieldValueParser.parseChar(bytesToParse, startIndex));
                break;
            case CHAR_ARRAY:
                checkTmpCharBuffer(length);
                FieldValueParser.readChars(bytesToParse, startIndex, length, tempBuffer, tempCharBuffer);
                fixMessage.setCharSequenceValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, tempCharBuffer, length);
                break;
            case DOUBLE:
                checkTmpCharBuffer(length);
                FieldValueParser.setDoubleValuesFromByteBufComposer(bytesToParse, startIndex, length, tempBuffer, tempCharBuffer,
                                                                    (unscaledValue, scale) -> fixMessage.setDoubleValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, unscaledValue, scale));
                break;
            case LONG:
            case GROUP:
                fixMessage.setLongValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, FieldValueParser.parseLong(bytesToParse, startIndex, io.github.zlooo.fixyou.model.FixMessage.FIELD_SEPARATOR));
                break;
            case TIMESTAMP:
                timestampParser.reset();
                fixMessage.setTimestampValue(fieldNumber, groupNumber, repetitionIndex, parentGroupRepetitionIndex, DateUtils.parseTimestamp(bytesToParse, startIndex, length, timestampParser));
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_FIELD_TYPE + fieldType);
        }
    }

    private void checkTmpCharBuffer(int length) {
        if (tempCharBuffer.length < length) {
            ReflectionUtils.setFinalField(this, Fields.tempCharBuffer, new char[length]);
        }
    }

    public boolean isDone() {
        return fixMessage.isValueSet(FixConstants.CHECK_SUM_FIELD_NUMBER);
    }
}
