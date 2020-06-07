package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.commons.ReusableCharArray;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import lombok.*;

import java.nio.charset.StandardCharsets;

@EqualsAndHashCode
@ToString
public abstract class AbstractField implements Closeable {

    @Getter
    protected final ByteBuf encodedFieldNumber;
    @Getter
    protected final int number;
    @Setter(AccessLevel.PROTECTED)
    protected ByteBuf fieldData;
    protected int startIndex;
    protected int endIndexIndex;
    protected boolean valueSet;

    public AbstractField(int number) {
        this.number = number;
        final ReusableCharArray fieldNumberAsChar = FieldUtils.toCharSequence(number);
        final int encodedFieldNumberCapacity = fieldNumberAsChar.length() + 1;
        encodedFieldNumber = Unpooled.directBuffer(encodedFieldNumberCapacity, encodedFieldNumberCapacity);
        encodedFieldNumber.writeCharSequence(fieldNumberAsChar, StandardCharsets.US_ASCII);
        encodedFieldNumber.writeByte(FixMessage.FIELD_VALUE_SEPARATOR);
        ReferenceCountUtil.release(fieldNumberAsChar);
    }

    public void setIndexes(int newStartIndex, int newEndIndexIndex) {
        this.startIndex = newStartIndex;
        this.endIndexIndex = newEndIndexIndex;
        this.valueSet = true;
    }

    public boolean isValueSet() {
        return valueSet;
    }

    public int getLength() {
        return endIndexIndex - startIndex;
    }

    public abstract FieldType getFieldType();

    protected abstract void resetInnerState();

    public void reset() {
        if (valueSet) {
            resetInnerState();
        }
        this.encodedFieldNumber.readerIndex(0);
        this.startIndex = 0;
        this.endIndexIndex = 0;
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
