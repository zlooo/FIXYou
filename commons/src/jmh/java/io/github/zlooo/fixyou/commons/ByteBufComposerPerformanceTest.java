package io.github.zlooo.fixyou.commons;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ThreadLocalRandom;

@Fork(1)
public class ByteBufComposerPerformanceTest {

    public static final int INITIAL_CAPACITY = 65536;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public int indexOf(TestState testState) {
        return testState.buf.indexOf(0, testState.buf.writerIndex(), testState.valueToFind);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public int byteProcessor(TestState testState) {
        return testState.buf.forEachByte(testState.byteProcessor);
    }

    @State(Scope.Benchmark)
    public static class TestState {
        private ByteBuf buf = Unpooled.directBuffer(INITIAL_CAPACITY);
        private byte valueToFind = (byte) ThreadLocalRandom.current().nextInt();
        private ByteProcessor byteProcessor = new ByteProcessor.IndexOfProcessor(valueToFind);

        @Setup
        public void setup() {
            final byte[] data = new byte[INITIAL_CAPACITY];
            ThreadLocalRandom.current().nextBytes(data);
            buf.writeBytes(data);
        }
    }
}
