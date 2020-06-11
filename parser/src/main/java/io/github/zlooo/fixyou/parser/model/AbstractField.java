package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.*;

import java.nio.charset.StandardCharsets;

@EqualsAndHashCode
@ToString(exclude = {"encodedFieldNumber", "fieldData"})
public abstract class AbstractField implements Closeable {

    @Getter
    protected final ByteBuf encodedFieldNumber;
    @Getter
    protected final int number;
    @Setter(AccessLevel.PROTECTED)
    protected ByteBuf fieldData;
    @Getter
    protected int startIndex;
    @Getter
    protected int endIndex;
    protected boolean valueSet;

    public AbstractField(int number) {
        this.number = number;
        final String fieldNumberAsString = Integer.toString(number); //we're doing it just on startup so we can afford it
        final int encodedFieldNumberCapacity = fieldNumberAsString.length() + 1;
        encodedFieldNumber = Unpooled.directBuffer(encodedFieldNumberCapacity, encodedFieldNumberCapacity);
        encodedFieldNumber.writeCharSequence(fieldNumberAsString, StandardCharsets.US_ASCII);
        encodedFieldNumber.writeByte(FixMessage.FIELD_VALUE_SEPARATOR);
    }

    public void setIndexes(int newStartIndex, int newEndIndexIndex) {
        this.startIndex = newStartIndex;
        this.endIndex = newEndIndexIndex;
        this.valueSet = true;
    }

    public boolean isValueSet() {
        return valueSet;
    }

    public int getLength() {
        return endIndex - startIndex;
    }

    public abstract FieldType getFieldType();

    protected abstract void resetInnerState();

    public void reset() {
        if (valueSet) {
            resetInnerState();
        }
        this.encodedFieldNumber.readerIndex(0);
        this.startIndex = 0;
        this.endIndex = 0;
        this.valueSet = false;
    }

    @Override
    public void close() {
        final int encodedFieldNumberRefCount = encodedFieldNumber.refCnt();
        if (encodedFieldNumberRefCount > 0) {
            encodedFieldNumber.release(encodedFieldNumberRefCount);
        }
    }

    public abstract void appendByteBufWithValue(ByteBuf out);
}
