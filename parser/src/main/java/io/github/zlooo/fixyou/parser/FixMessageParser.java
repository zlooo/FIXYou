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
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.parser.model.AbstractField;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.parser.model.GroupField;
import io.github.zlooo.fixyou.parser.model.ParsingUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@NoArgsConstructor
public class FixMessageParser {

    private static final ByteProcessor FIELD_TERMINATOR_FINDER = new ByteProcessor.IndexOfProcessor(FixMessage.FIELD_SEPARATOR_BYTE);
    private static final String FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG = "Field {} not found in message spec";
    private static final int NOT_FOUND = -1;
    private static final int MAX_CAPACITY_REACHED = 3;
    private static final int NOT_ENOUGH_BYTES_IN_BUFFER = 1;
    private ByteBuf parseableBytes = Unpooled.EMPTY_BUFFER; //TODO think about custom implementation of CompositeByteBuf, maybe it'll come in handy here? You wouldn't have to copy incoming data
    @Getter
    @Setter
    private FixMessage fixMessage;
    @Getter
    private boolean parseable;
    private boolean parsingRepeatingGroup;
    private final Deque<GroupField> groupFieldsStack = new ArrayDeque<>(DefaultConfiguration.NESTED_REPEATING_GROUPS);

    public FixMessageParser(FixMessage fixMessage) {
        this.fixMessage = fixMessage;
    }

    public void setFixBytes(@Nonnull ByteBuf fixMsgBufBytes) {
        parseable = true;
        if (parseableBytes.writerIndex() == 0) {
            parseableBytes.release();
            fixMsgBufBytes.retain();
            parseableBytes = fixMsgBufBytes;
        } else {
            final int ensureWritableResult = parseableBytes.ensureWritable(fixMsgBufBytes.readableBytes(), true);
            log.debug("Ensure writable result is {}", ensureWritableResult);
            if (ensureWritableResult == NOT_ENOUGH_BYTES_IN_BUFFER || ensureWritableResult == MAX_CAPACITY_REACHED) {
                final ByteBuf oldbuffer = this.parseableBytes;
                this.parseableBytes =
                        fixMsgBufBytes.alloc().directBuffer(parseableBytes.readableBytes() + fixMsgBufBytes.writerIndex()).writeBytes(this.parseableBytes, this.parseableBytes.readerIndex(), this.parseableBytes.readableBytes());
                oldbuffer.release();
            }
            parseableBytes.writeBytes(fixMsgBufBytes);
        }
    }

    public boolean isUnderlyingBufferReadable() {
        return parseableBytes.isReadable();
    }

    public void parseFixMsgBytes() {
        int closestFieldTerminatorIndex;

        while ((closestFieldTerminatorIndex = parseableBytes.forEachByte(FIELD_TERMINATOR_FINDER)) != NOT_FOUND) {
            final int fieldNum = ParsingUtils.parseInteger(parseableBytes, FixMessage.FIELD_SEPARATOR_BYTE);
            AbstractField field = null;
            if (!parsingRepeatingGroup) {
                field = fixMessage.getField(fieldNum);
            } else {
                final GroupField groupField = groupFieldsStack.peek();
                if (groupField.containsField(fieldNum)) { //quick path, in 90% cases we won't have to deal with nested repeating groups
                    field = groupField.getFieldAndIncRepetitionIfValueIsSet(fieldNum);
                } else {
                    field = handleNestedRepeatingGroup(fieldNum);
                }
            }
            final int start = parseableBytes.readerIndex();
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
                if (parseableBytes.writerIndex() == parseableBytes.readerIndex()) {
                    parseableBytes.release();
                    parseableBytes = Unpooled.EMPTY_BUFFER;
                }
                fixMessage.setMessageByteSource(parseableBytes);
                return;
            }
        }
        parseable = false;
    }

    private AbstractField handleNestedRepeatingGroup(int fieldNum) {
        GroupField groupField;
        while ((groupField = groupFieldsStack.peek()) != null) {
            if (groupField.containsField(fieldNum)) {
                return groupField.getFieldAndIncRepetitionIfValueIsSet(fieldNum);
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
