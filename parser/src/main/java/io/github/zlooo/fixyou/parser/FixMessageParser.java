package io.github.zlooo.fixyou.parser;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.model.Field;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.parser.model.ParsingUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntHashSet;

import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
public class FixMessageParser implements Resettable {

    private static final String FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG = "Field {} not found in message spec";
    @Getter
    private final ByteBufComposer bytesToParse;
    private final Int2ObjectHashMap<IntHashSet> groupFieldsConstituents = new Int2ObjectHashMap<>();
    @Getter
    private final FixMessage fixMessage;
    private boolean parsingRepeatingGroup;
    private final Deque<Field> groupFieldsStack = new ArrayDeque<>(DefaultConfiguration.NESTED_REPEATING_GROUPS);
    private int storedEndIndexOfLastUnfinishedMessage;

    public FixMessageParser(ByteBufComposer bytesToParse, FixSpec fixSpec, FixMessage fixMessage) {
        this.bytesToParse = bytesToParse;
        this.fixMessage = fixMessage;
        final int[] fieldsOrder = fixSpec.getFieldsOrder();
        for (final int fieldNumber : fieldsOrder) {
            final int[] groupFieldNumbers = fixSpec.getRepeatingGroupFieldNumbers(fieldNumber);
            if (groupFieldNumbers.length > 0) {
                final IntHashSet groupFieldConstituents = new IntHashSet();
                for (final int groupFieldNumber : groupFieldNumbers) {
                    groupFieldConstituents.add(groupFieldNumber);
                }
                this.groupFieldsConstituents.put(fieldNumber, groupFieldConstituents);
            }
        }
    }

    @Override
    public void reset() {
        fixMessage.resetAllDataFieldsAndReleaseByteSource();
        parsingRepeatingGroup = false;
        groupFieldsStack.clear();
        storedEndIndexOfLastUnfinishedMessage = 0;
    }

    public void startParsing() {
        this.fixMessage.setStartIndex(bytesToParse.readerIndex());
        storedEndIndexOfLastUnfinishedMessage = 0;
    }

    public void parseFixMsgBytes() {
        int closestFieldTerminatorIndex;

        while (!bytesToParse.readerIndexBeyondStoredEnd() && ((closestFieldTerminatorIndex = bytesToParse.indexOfClosestSOH()) != ByteBufComposer.NOT_FOUND)) {
            final int fieldNum = ParsingUtils.parseInteger(bytesToParse, bytesToParse.readerIndex(), FixMessage.FIELD_VALUE_SEPARATOR, true);
            final int start = bytesToParse.readerIndex();
            Field field;
            if (!parsingRepeatingGroup) {
                field = fixMessage.getField(fieldNum);
            } else {
                final Field groupField = groupFieldsStack.peek();
                if (groupFieldsConstituents.get(groupField.getNumber()).contains(fieldNum)) { //quick path, in 90% cases we won't have to deal with nested repeating groups
                    field = groupField.getFieldForCurrentRepetition(fieldNum);
                    if (field.isValueSet()) {
                        field = groupField.endCurrentRepetition().getFieldForCurrentRepetition(fieldNum);
                    }
                } else {
                    field = handleNestedRepeatingGroup(fieldNum);
                }
            }
            bytesToParse.readerIndex(closestFieldTerminatorIndex + 1);
            if (field != null) {
                field.setIndexes(start, closestFieldTerminatorIndex);
                if (groupFieldsConstituents.containsKey(field.getNumber())) {
                    parsingRepeatingGroup = true;
                    groupFieldsStack.addFirst(field);
                }
            } else {
                log.debug(FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG, fieldNum);
            }
            if (fieldNum == FixConstants.CHECK_SUM_FIELD_NUMBER) {
                storedEndIndexOfLastUnfinishedMessage = 0;
                fixMessage.setEndIndex(closestFieldTerminatorIndex); //including last SOH
                return;
            }
        }
        storedEndIndexOfLastUnfinishedMessage = bytesToParse.getStoredEndIndex();
    }

    public boolean canContinueParsing() {
        return !bytesToParse.readerIndexBeyondStoredEnd() && (storedEndIndexOfLastUnfinishedMessage == 0 || storedEndIndexOfLastUnfinishedMessage < bytesToParse.getStoredEndIndex());
    }

    private Field handleNestedRepeatingGroup(int fieldNum) {
        Field groupField;
        while ((groupField = groupFieldsStack.peek()) != null) {
            if (groupFieldsConstituents.get(groupField.getNumber()).contains(fieldNum)) {
                Field currentRepetition = groupField.getFieldForCurrentRepetition(fieldNum);
                if (currentRepetition.isValueSet()) {
                    currentRepetition = groupField.endCurrentRepetition().getFieldForCurrentRepetition(fieldNum);
                }
                return currentRepetition;
            } else {
                groupFieldsStack.poll();
                groupField.endCurrentRepetition();
            }
        }
        parsingRepeatingGroup = false;
        return fixMessage.getField(fieldNum);
    }

    public boolean isDone() {
        return fixMessage.getField(FixConstants.CHECK_SUM_FIELD_NUMBER).isValueSet();
    }
}
