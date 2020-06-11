package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.utils.DateUtils;
import io.github.zlooo.fixyou.model.FieldType;
import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class TimestampField extends AbstractField {

    public static final long DEFAULT_VALUE = Long.MIN_VALUE;
    private final MutableCharSequence charSequence = new MutableCharSequence();
    private long value = DEFAULT_VALUE;
    private byte[] rawValue = new byte[FixConstants.UTC_TIMESTAMP_PATTERN.length()];
    private char[] charValue = new char[FixConstants.UTC_TIMESTAMP_PATTERN.length()];

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
            fieldData.readerIndex(startIndex);
            final int length = endIndex - startIndex;
            ParsingUtils.readChars(fieldData, length, rawValue, charValue);
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
    }

    public void setValue(long value) {
        this.value = value;
        this.valueSet = true;
    }

    @Override
    public void appendByteBufWithValue(ByteBuf out) {
        DateUtils.writeTimestamp(getValue(), out, true);
    }
}
