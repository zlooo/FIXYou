package io.github.zlooo.fixyou.netty.handler;

import com.carrotsearch.hppcrt.IntObjectMap;
import com.carrotsearch.hppcrt.cursors.IntCursor;
import io.github.zlooo.fixyou.model.ExtendedFixSpec;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
class UnorderedMessageEncoder extends AbstractMessageEncoder {

    @Inject
    UnorderedMessageEncoder() {
    }

    @Override
    protected int writeFields(FixMessage msg, ByteBuf out, ExtendedFixSpec fixSpec) {
        int sumOfBytes = 0;
        final IntObjectMap<FieldType> fieldNumberToType = fixSpec.getFieldNumberToType();
        for (final IntCursor setField : msg.setFields()) {
            final int fieldNumber = setField.value;
            if (!fixSpec.getHeaderFieldNumbers().containsKey(fieldNumber)) {
                final FieldType fieldType = fieldNumberToType.get(fieldNumber);
                if (fieldType != null) { //if null it means we have either content of repeating group or unknown field, either way we don't want to write this field
                    sumOfBytes += writeTagAndSeparator(fieldNumber, out);
                    sumOfBytes += writeValue(msg, fixSpec, fieldNumber, fieldType, out);
                    out.writeByte(io.github.zlooo.fixyou.model.FixMessage.FIELD_SEPARATOR);
                    sumOfBytes += io.github.zlooo.fixyou.model.FixMessage.FIELD_SEPARATOR;
                }
            }
        }
        return sumOfBytes;
    }
}
