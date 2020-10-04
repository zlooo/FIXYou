package io.github.zlooo.fixyou.parser.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;

@Fork(1)
public class FieldPerformanceTest {

    private static final int SINK_BUF_CAPACITY = 512;

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public ByteBuf array(TestState state) {
        return state.sink.writeBytes(state.array).clear();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public ByteBuf heap(TestState state) {
        return state.sink.writeBytes(state.heapByteBuf, 0, state.dataToWriteLength).clear();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public ByteBuf direct(TestState state) {
        return state.sink.writeBytes(state.directByteBuf, 0, state.dataToWriteLength).clear();
    }

    @State(Scope.Benchmark)
    public static class TestState {
        @Param({"5", "10", "50", "200"})
        private int dataToWriteLength;
        private byte[] array;
        private ByteBuf heapByteBuf;
        private ByteBuf directByteBuf;
        private ByteBuf sink = Unpooled.directBuffer(SINK_BUF_CAPACITY, Integer.MAX_VALUE);

        @Setup
        public void setup() {
            array = RandomStringUtils.randomAlphanumeric(dataToWriteLength).getBytes(StandardCharsets.US_ASCII);
            heapByteBuf = Unpooled.buffer(dataToWriteLength, dataToWriteLength);
            directByteBuf = Unpooled.directBuffer(dataToWriteLength, dataToWriteLength);
            heapByteBuf.writeBytes(array);
            directByteBuf.writeBytes(array);
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            sink.clear();
        }
    }
}
