package io.github.zlooo.fixyou.parser.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;

@Fork(1)
public class FieldPerformanceTest {

    private static final int BUF_CAPACITY = 5;
    private static final int SINK_BUF_CAPACITY = 512;

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public ByteBuf array(TestState state) {
        return state.sink.writeBytes(state.array).clear();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public ByteBuf heap(TestState state) {
        return state.sink.writeBytes(state.heapByteBuf).clear();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public ByteBuf direct(TestState state) {
        return state.sink.writeBytes(state.directByteBuf).clear();
    }

    @State(Scope.Benchmark)
    public static class TestState {
        private byte[] array = "1234=".getBytes(StandardCharsets.US_ASCII);
        private ByteBuf heapByteBuf = Unpooled.buffer(BUF_CAPACITY, BUF_CAPACITY);
        private ByteBuf directByteBuf = Unpooled.directBuffer(BUF_CAPACITY, BUF_CAPACITY);
        private ByteBuf sink = Unpooled.directBuffer(SINK_BUF_CAPACITY, Integer.MAX_VALUE);

        @Setup
        public void setup() {
            heapByteBuf.writeBytes(array);
            directByteBuf.writeBytes(array);
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            sink.clear();
        }
    }
}
