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
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.parser.model.AbstractField;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.parser.model.GroupField;
import io.github.zlooo.fixyou.parser.model.ParsingUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@NoArgsConstructor
public class FixMessageParser implements Resettable {

    private static final ByteProcessor FIELD_TERMINATOR_FINDER = new ByteProcessor.IndexOfProcessor(FixMessage.FIELD_SEPARATOR);
    private static final String FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG = "Field {} not found in message spec";
    private static final int NOT_FOUND = -1;
    private ByteBuf parseableBytes = Unpooled.EMPTY_BUFFER;
    @Getter
    private FixMessage fixMessage;
    @Getter
    private boolean parseable;
    private boolean fragmentationDetected;
    private int lastBeginStringIndex;
    private boolean parsingRepeatingGroup;
    private final Deque<GroupField> groupFieldsStack = new ArrayDeque<>(DefaultConfiguration.NESTED_REPEATING_GROUPS);

    @Override
    public void reset() {
        parseableBytes.release();
        parseableBytes = Unpooled.EMPTY_BUFFER;
        if (fixMessage != null) {
            fixMessage.release();
            fixMessage = null;
        }
        parseable = false;
        fragmentationDetected = false;
        lastBeginStringIndex = 0;
        parsingRepeatingGroup = false;
        groupFieldsStack.clear();
    }

    public void setFixBytes(@Nonnull ByteBuf fixMsgBufBytes) {
        parseable = true;
        fixMsgBufBytes.retain();
        if (parseableBytes.writerIndex() == 0) {
            parseableBytes.release();
            parseableBytes = fixMsgBufBytes;
        } else {
            /*
            If we're here this means we're dealing with fragmentation. This means following should be true:
            1) fixMessage should already be set
            2) current parseableBytes should contain first part of message
            3) fixMsgBufBytes should contain rest of message or at least another part of it
             */
            if (fixMessage != null) {
                final int alreadyParsedOffset = parseableBytes.readerIndex() - lastBeginStringIndex;
                parseableBytes.readerIndex(lastBeginStringIndex);
                for (final AbstractField field : fixMessage.getFieldsOrdered()) {
                    if (field.isValueSet()) {
                        reindexField(field, lastBeginStringIndex);
                    }
                }
                final CompositeByteBuf fragmentationBuffer = fixMsgBufBytes.alloc().compositeBuffer(2);
                /*
                we should retain parseableBytes since CompositeByteBuf will call release on it but I'd be calling release anyway couple of lines later since this class no longer holds reference to parseableBytes after
                parseableBytes = fragmentationBuffer is executed so no need to do that
                 */
                fragmentationBuffer.addComponent(true, parseableBytes);
                fixMsgBufBytes.retain(); //that's because when releasing fragmentationBuffer when message is parsed to the end, this ByteBuf will be released as well
                fragmentationBuffer.addComponent(true, fixMsgBufBytes);
                fragmentationBuffer.readerIndex(alreadyParsedOffset);
                parseableBytes = fragmentationBuffer;
                fixMessage.setMessageByteSourceAndRetain(fragmentationBuffer);
                fragmentationDetected = true;
            } else {
                log.error("FixMessage is not set? At this point we're supposed to be in the middle of parsing. Resetting parser");
                parseableBytes.release();
                fixMsgBufBytes.release();
                parseableBytes = Unpooled.EMPTY_BUFFER;
            }
        }
    }

    private void reindexField(AbstractField field, int indexOffset) {
        field.setIndexes(field.getStartIndex() - indexOffset, field.getEndIndex() - indexOffset);
    }

    public void setFixMessage(FixMessage fixMessage) {
        this.fixMessage = fixMessage;
        this.lastBeginStringIndex = 0;
        if (fixMessage != null) {
            this.fixMessage.setMessageByteSourceAndRetain(parseableBytes);
        }
    }

    public boolean isUnderlyingBufferReadable() {
        return parseableBytes.isReadable();
    }

    public void parseFixMsgBytes() {
        int closestFieldTerminatorIndex;

        while ((closestFieldTerminatorIndex = parseableBytes.forEachByte(FIELD_TERMINATOR_FINDER)) != NOT_FOUND) {
            final int fieldNum = ParsingUtils.parseInteger(parseableBytes, parseableBytes.readerIndex(), FixMessage.FIELD_VALUE_SEPARATOR, true);
            final int start = parseableBytes.readerIndex();
            saveIndexIfBeginString(fieldNum, start);
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
            parseableBytes.readerIndex(closestFieldTerminatorIndex + 1);
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
                endOfMessage();
                return;
            }
        }
        parseable = false;
    }

    private void endOfMessage() {
        if (fragmentationDetected) {
            fragmentationDetected = false;
            final CompositeByteBuf fragmentationBuffer = (CompositeByteBuf) parseableBytes;
            final int bytesRead = fragmentationBuffer.readerIndex();
            parseableBytes = fragmentationBuffer.component(1);
            final int firstFragmentReadableBytes = fragmentationBuffer.component(0).readableBytes();
            parseableBytes.readerIndex(bytesRead - firstFragmentReadableBytes);
            fragmentationBuffer.release();
        }
        if (parseableBytes.writerIndex() == parseableBytes.readerIndex()) {
            parseableBytes.release();
            parseableBytes = Unpooled.EMPTY_BUFFER;
        }
        lastBeginStringIndex = 0;
    }

    private void saveIndexIfBeginString(int fieldNum, int start) {
        if (fieldNum == FixConstants.BEGIN_STRING_FIELD_NUMBER) {
            lastBeginStringIndex = start;
        }
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
