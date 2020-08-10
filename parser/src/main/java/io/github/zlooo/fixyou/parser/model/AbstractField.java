package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import lombok.*;

@EqualsAndHashCode
@ToString(exclude = {"encodedFieldNumber", "fieldData"})
public abstract class AbstractField implements Closeable {

    @Getter
    protected final int number;
    @Setter(AccessLevel.PROTECTED)
    protected ByteBufComposer fieldData;
    @Getter
    protected volatile int startIndex;
    @Getter
    protected volatile int endIndex;
    protected volatile boolean valueSet;
    @Getter(value = AccessLevel.PROTECTED)
    private final byte[] encodedFieldNumber;
    @Getter
    private final int encodedFieldNumberLength;

    public AbstractField(int number) {
        this.number = number;
        final char[] fieldNumberAsCharArray = Integer.toString(number).toCharArray(); //we're doing it just on startup so we can afford it
        final int encodedFieldNumberCapacity = fieldNumberAsCharArray.length + 1;
        encodedFieldNumber = new byte[encodedFieldNumberCapacity];
        for (int i = 0; i < fieldNumberAsCharArray.length; i++) {
            encodedFieldNumber[i] = AsciiString.c2b(fieldNumberAsCharArray[i]);
        }
        encodedFieldNumber[encodedFieldNumberCapacity - 1] = FixMessage.FIELD_VALUE_SEPARATOR;
        encodedFieldNumberLength = encodedFieldNumber.length;
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
        this.startIndex = 0;
        this.endIndex = 0;
        this.valueSet = false;
    }

    @Override
    public void close() {
        //nothing to do
    }

    public void appendFieldNumber(ByteBuf out) {
        out.writeBytes(encodedFieldNumber);
    }

    public abstract void appendByteBufWithValue(ByteBuf out);
}
