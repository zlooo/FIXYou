package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.utils.DateUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class TimestampField extends AbstractField {

    public static final long DEFAULT_VALUE = Long.MIN_VALUE;
    private static final int ENCODED_TIMESTAMP_LENGTH = FixConstants.UTC_TIMESTAMP_PATTERN.length();
    private final MutableCharSequence charSequence = new MutableCharSequence();
    private final byte[] rawValue = new byte[ENCODED_TIMESTAMP_LENGTH];
    private final ByteBuf rawValueAsBuffer = Unpooled.wrappedBuffer(rawValue);
    private final char[] charValue = new char[FixConstants.UTC_TIMESTAMP_PATTERN.length()];
    private long value = DEFAULT_VALUE;
    private int sumOfBytes;

    public TimestampField(int number) {
        super(number);
        charSequence.setState(charValue);
    }

    @Override
    public FieldType getFieldType() {
        return FieldType.TIMESTAMP;
    }

    public long getValue() {
        if (value == DEFAULT_VALUE && valueSet) { //TODO seriously? write a method that parses directly to epoch timestamp you lazy son of a bitch
            //TODO it's last place that uses formatter get rid of it when you refactor this code
            final int length = endIndex - startIndex;
            ParsingUtils.readChars(fieldData, startIndex, length, rawValue, charValue);
            charSequence.setLength(length);
            value = chooseFormatter(length).parse(charSequence, LocalDateTime::from).toInstant(ZoneOffset.UTC).toEpochMilli();
            valueSet = true;
        }
        return value;
    }

    private DateTimeFormatter chooseFormatter(int length) {
        if (FixConstants.UTC_TIMESTAMP_PATTERN.length() == length) {
            return FixConstants.UTC_TIMESTAMP_FORMATTER;
        } else {
            return FixConstants.UTC_TIMESTAMP_NO_MILLIS_FORMATTER;
        }
    }

    @Override
    protected void resetInnerState() {
        value = DEFAULT_VALUE;
        sumOfBytes = 0;
        rawValueAsBuffer.clear();
    }

    public void setValue(long value) {
        this.value = value;
        this.valueSet = true;
        sumOfBytes = DateUtils.writeTimestamp(value, rawValueAsBuffer.clear(), true);
    }

    @Override
    public int appendByteBufWithValue(ByteBuf out) {
        out.writeBytes(rawValueAsBuffer, 0, ENCODED_TIMESTAMP_LENGTH);
        return sumOfBytes;
    }
}
