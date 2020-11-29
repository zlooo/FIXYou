package io.github.zlooo.fixyou.parser.cache;

import io.github.zlooo.fixyou.commons.utils.FieldUtils;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Value;
import org.agrona.collections.Int2ObjectHashMap;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class FieldNumberCache {

    private static final int BUFFER_INITIAL_CAPACITY = 5;
    private final Int2ObjectHashMap<ByteBufWithSum> cache = new Int2ObjectHashMap<>();
    private final AtomicReference<Executor> executorRef = new AtomicReference<>();

    public ByteBufWithSum getEncodedFieldNumber(int fieldNumber, Executor executor) {
        ByteBufWithSum byteBufWithSum = cache.get(fieldNumber);
        if (byteBufWithSum == null) {
            final ByteBuf byteBuf = Unpooled.directBuffer(BUFFER_INITIAL_CAPACITY);
            final int sumOfBytes = FieldUtils.writeEncoded(fieldNumber, byteBuf);
            byteBuf.writeByte(FixMessage.FIELD_VALUE_SEPARATOR);
            byteBufWithSum = new ByteBufWithSum(byteBuf, sumOfBytes + FixMessage.FIELD_VALUE_SEPARATOR);
            enqueuePutTask(fieldNumber, byteBufWithSum, executor);
        }
        return byteBufWithSum;
    }

    private void enqueuePutTask(int fieldNumber, ByteBufWithSum byteBufWithSum, Executor executor) {
        executorRef.compareAndSet(null, executor); //don't care about result, I'm fine either way. All I care is that after this line ref != null
        executorRef.get().execute(() -> {
            final ByteBufWithSum previousValue = cache.put(fieldNumber, byteBufWithSum);
            if (previousValue != null) {
                previousValue.byteBuf.release();
            }
        });
    }

    @Value
    public static final class ByteBufWithSum {
        private final ByteBuf byteBuf;
        private final int sumOfBytes;
    }
}
