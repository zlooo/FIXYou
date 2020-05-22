package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.commons.ReusableCharArray;
import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.ToString;

import java.nio.charset.StandardCharsets;

@Getter
@ToString
public abstract class AbstractField implements Closeable {
    public static final byte FIELD_TERMINATOR = 1;
    private static final byte FIELD_VALUE_SEPARATOR = 61;

    protected final ByteBuf encodedFieldNumber;
    protected final ByteBuf fieldData;
    protected final int number;

    public AbstractField(int number, int fieldDataLength) {
        this.fieldData = Unpooled.directBuffer(fieldDataLength);
        this.number = number;
        final ReusableCharArray fieldNumberAsChar = FieldUtils.toCharSequence(number);
        encodedFieldNumber = Unpooled.directBuffer(fieldNumberAsChar.length() + 1);
        encodedFieldNumber.writeCharSequence(fieldNumberAsChar, StandardCharsets.US_ASCII);
        encodedFieldNumber.writeByte(FIELD_VALUE_SEPARATOR);
        ReferenceCountUtil.release(fieldNumberAsChar);
    }

    public abstract FieldType getFieldType();

    protected abstract void resetInnerState();

    public void setFieldData(ByteBuf fieldData) {
        this.fieldData.clear();
        this.fieldData.writeBytes(fieldData.readerIndex(0));
    }

    public void setFieldData(byte[] bytes) {
        this.fieldData.clear();
        this.fieldData.writeBytes(bytes);
    }

    public boolean isValueSet() {
        return this.fieldData.writerIndex() > 0;
    }

    public void reset() {
        if (isValueSet()) {
            resetInnerState();
        }
        this.encodedFieldNumber.readerIndex(0);
        this.fieldData.clear();
    }

    @Override
    public void close() {
        final int encodedFieldNumberRefCount = encodedFieldNumber.refCnt();
        if (encodedFieldNumberRefCount > 0) {
            encodedFieldNumber.release(encodedFieldNumberRefCount);
        }
        final int fieldDataRefCount = fieldData.refCnt();
        if (fieldDataRefCount > 0) {
            fieldData.release(fieldDataRefCount);
        }
    }
}
