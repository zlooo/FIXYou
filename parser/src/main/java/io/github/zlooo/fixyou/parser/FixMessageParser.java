/*
 * Copyright 2015 peter.lawrey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2020 zlooo
 */

package io.github.zlooo.fixyou.parser;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.parser.model.AbstractField;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.parser.model.GroupField;
import io.github.zlooo.fixyou.parser.model.ParsingUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@RequiredArgsConstructor
public class FixMessageParser implements Resettable {

    private static final String FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG = "Field {} not found in message spec";
    @Getter
    private final ByteBufComposer bytesToParse;
    @Getter
    private FixMessage fixMessage;
    private boolean parsingRepeatingGroup;
    private final Deque<GroupField> groupFieldsStack = new ArrayDeque<>(DefaultConfiguration.NESTED_REPEATING_GROUPS);
    private int storedEndIndexOfLastUnfinishedMessage;

    @Override
    public void reset() {
        if (fixMessage != null) {
            fixMessage.release();
            fixMessage = null;
        }
        parsingRepeatingGroup = false;
        groupFieldsStack.clear();
        storedEndIndexOfLastUnfinishedMessage = 0;
    }

    public void setFixMessage(FixMessage fixMessage) {
        this.fixMessage = fixMessage;
        if (fixMessage != null) {
            this.fixMessage.setMessageByteSource(bytesToParse);
        }
        storedEndIndexOfLastUnfinishedMessage = 0;
    }

    public void parseFixMsgBytes() {
        int closestFieldTerminatorIndex;

        while ((closestFieldTerminatorIndex = bytesToParse.indexOfClosest(FixMessage.FIELD_SEPARATOR)) != ByteBufComposer.VALUE_NOT_FOUND) {
            final int fieldNum = ParsingUtils.parseInteger(bytesToParse, bytesToParse.readerIndex(), FixMessage.FIELD_VALUE_SEPARATOR, true);
            final int start = bytesToParse.readerIndex();
            AbstractField field = null;
            if (!parsingRepeatingGroup) {
                field = fixMessage.getField(fieldNum);
            } else {
                final GroupField groupField = groupFieldsStack.peek();
                if (groupField.containsField(fieldNum)) { //quick path, in 90% cases we won't have to deal with nested repeating groups
                    field = groupField.getFieldForCurrentRepetition(fieldNum);
                    if (field.isValueSet()) {
                        field = groupField.next().getFieldForCurrentRepetition(fieldNum);
                    }
                } else {
                    field = handleNestedRepeatingGroup(fieldNum);
                }
            }
            bytesToParse.readerIndex(closestFieldTerminatorIndex + 1);
            if (field != null) {
                field.setIndexes(start, closestFieldTerminatorIndex);
                if (field.getFieldType() == FieldType.GROUP) {
                    parsingRepeatingGroup = true;
                    final GroupField groupField = (GroupField) field;
                    groupFieldsStack.addFirst(groupField);
                }
            } else {
                log.debug(FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG, fieldNum);
            }
            if (fieldNum == FixConstants.CHECK_SUM_FIELD_NUMBER) {
                storedEndIndexOfLastUnfinishedMessage = 0;
                return;
            }
        }
        storedEndIndexOfLastUnfinishedMessage = bytesToParse.getStoredEndIndex();
    }

    public boolean canContinueParsing() {
        return bytesToParse.readerIndex() < bytesToParse.getStoredEndIndex() && (storedEndIndexOfLastUnfinishedMessage == 0 || storedEndIndexOfLastUnfinishedMessage < bytesToParse.getStoredEndIndex());
    }

    private AbstractField handleNestedRepeatingGroup(int fieldNum) {
        GroupField groupField;
        while ((groupField = groupFieldsStack.peek()) != null) {
            if (groupField.containsField(fieldNum)) {
                AbstractField currentRepetition = groupField.getFieldForCurrentRepetition(fieldNum);
                if (currentRepetition.isValueSet()) {
                    currentRepetition = groupField.next().getFieldForCurrentRepetition(fieldNum);
                }
                return currentRepetition;
            } else {
                groupFieldsStack.poll();
            }
        }
        parsingRepeatingGroup = false;
        return fixMessage.getField(fieldNum);
    }

    public boolean isDone() {
        return fixMessage.getField(FixConstants.CHECK_SUM_FIELD_NUMBER).isValueSet();
    }
}
