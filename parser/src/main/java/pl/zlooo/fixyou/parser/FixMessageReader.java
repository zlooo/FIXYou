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

package pl.zlooo.fixyou.parser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import pl.zlooo.fixyou.DefaultConfiguration;
import pl.zlooo.fixyou.FixConstants;
import pl.zlooo.fixyou.model.FieldType;
import pl.zlooo.fixyou.parser.model.AbstractField;
import pl.zlooo.fixyou.parser.model.FixMessage;
import pl.zlooo.fixyou.parser.model.GroupField;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Deque;

@Slf4j
@NoArgsConstructor
public class FixMessageReader {

    private static final int RADIX = 10;
    private static final ByteProcessor FIELD_TERMINATOR_FINDER = new ByteProcessor.IndexOfProcessor(AbstractField.FIELD_TERMINATOR);
    private static final String FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG = "Field {} not found in message spec";
    private static final int NOT_FOUND = -1;
    private ByteBuf parseableBytes = Unpooled.buffer(0, 0);
    @Getter
    @Setter
    private FixMessage fixMessage;
    @Getter
    private boolean parseable;
    private boolean parsingRepeatingGroup;
    private final Deque<GroupField> groupFieldsStack = new ArrayDeque<>(DefaultConfiguration.NESTED_REPEATING_GROUPS);

    /**
     * @param fixMessage FixMessage object to be read/parsed by the object of this class, at the time of initialization.
     */
    public FixMessageReader(FixMessage fixMessage) {
        this.fixMessage = fixMessage;
    }

    /**
     * Accepts a new Fix Message ByteBufferBytes. This method is a precursor to parseFixMsgBytes()
     *
     * @param fixMsgBufBytes - Fix message from client in bytes
     */
    public void setFixBytes(@Nonnull ByteBuf fixMsgBufBytes) {
        parseable = true;
        if (parseableBytes.writerIndex() == 0) {
            parseableBytes.release();
            fixMsgBufBytes.retain();
            parseableBytes = fixMsgBufBytes;
        } else {
            final int ensureWritableResult = parseableBytes.ensureWritable(fixMsgBufBytes.readableBytes(), true);
            log.debug("Ensure writable result is {}", ensureWritableResult);
            if (ensureWritableResult == 1 || ensureWritableResult == 3) {
                final ByteBuf oldbuffer = this.parseableBytes;
                this.parseableBytes =
                        fixMsgBufBytes.alloc().directBuffer(parseableBytes.readableBytes() + fixMsgBufBytes.writerIndex()).writeBytes(this.parseableBytes, this.parseableBytes.readerIndex(), this.parseableBytes.readableBytes());
                oldbuffer.release();
            }
            parseableBytes.writeBytes(fixMsgBufBytes);
            //            fixMsgBufBytes.readerIndex(fixMsgBufBytes.writerIndex());
        }
    }

    public boolean isUnderlyingBufferReadable() {
        return parseableBytes.isReadable();
    }

    public void parseFixMsgBytes() {
        int closestFieldTerminatorIndex;

        while ((closestFieldTerminatorIndex = parseableBytes.forEachByte(FIELD_TERMINATOR_FINDER)) != NOT_FOUND) {
            final int fieldNum = parseFieldNumer(parseableBytes);
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
                field.getFieldData().writeBytes(parseableBytes, start, closestFieldTerminatorIndex - start);
                if (field.getFieldType() == FieldType.GROUP) {
                    parsingRepeatingGroup = true;
                    final GroupField groupField = (GroupField) field;
                    groupField.parseRepetitionsNumber();
                    groupFieldsStack.addFirst(groupField);
                }
            } else {
                log.debug(FIELD_NOT_FOUND_IN_MESSAGE_SPEC_LOG, fieldNum);
            }
            if (fieldNum == FixConstants.CHECK_SUM_FIELD_NUMBER) {
                return;
            }
        }
        if (closestFieldTerminatorIndex == NOT_FOUND) {
            parseable = false;
        }
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

    private int parseFieldNumer(ByteBuf message) {
        int num = 0;
        boolean negative = false;
        while (true) {
            final short b = message.readUnsignedByte();
            if ((b - ('0' + Integer.MIN_VALUE)) <= 9 + Integer.MIN_VALUE) {
                num = num * RADIX + b - '0';
            } else if (b == '-') {
                negative = true;
            } else if (b == '=') {
                break;
            }
        }
        return negative ? -num : num;
    }

    public boolean isDone() {
        return fixMessage.getField(FixConstants.CHECK_SUM_FIELD_NUMBER).isValueSet();
    }
}
