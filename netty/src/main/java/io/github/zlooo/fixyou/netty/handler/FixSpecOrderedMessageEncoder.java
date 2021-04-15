package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.model.ExtendedFixSpec;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
class FixSpecOrderedMessageEncoder extends AbstractMessageEncoder {

    @Inject
    FixSpecOrderedMessageEncoder() {
    }

    @Override
    protected int writeFields(FixMessage msg, ByteBuf out, ExtendedFixSpec fixSpec) {
        final int[] fieldsOrder = fixSpec.getBodyFieldsOrder();
        final FieldType[] fieldTypes = fixSpec.getBodyFieldTypes();
        int sumOfBytes = 0;
        for (int i = 3; i < fieldsOrder.length - 1; i++) {
            final int fieldNumber = ArrayUtils.getElementAt(fieldsOrder, i);
            if (msg.isValueSet(fieldNumber)) {
                final io.github.zlooo.fixyou.model.FieldType fieldType = ArrayUtils.getElementAt(fieldTypes, i);
                //TODO check if bulk move, if 1 out.writeBytes per field, will perform better
                sumOfBytes += writeTagAndSeparator(fieldNumber, out);
                sumOfBytes += writeValue(msg, fixSpec, fieldNumber, fieldType, out);
                out.writeByte(FixMessage.FIELD_SEPARATOR);
                sumOfBytes += FixMessage.FIELD_SEPARATOR;
            }
        }
        return sumOfBytes;
    }
}
