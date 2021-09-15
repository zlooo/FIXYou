package io.github.zlooo.fixyou.commons.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.ThreadLocalRandom;

public class FieldUtilsPerformanceTest {

    private static final int BUF_LENGTH = 8;
    private static final long MAX_VALUE = 9999999;
    private static final int NUMBER_OF_VALUES_TO_WRITE = 500;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void baselineWriteEncodedTest(TestState testState) {
        for (final long value : testState.valuesToWrite) {
            BaselineFieldUtils.writeEncoded(value, testState.byteBuf.clear());
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void writeEncodedTest(TestState testState) {
        for (final long value : testState.valuesToWrite) {
            FieldUtils.writeEncoded(value, testState.byteBuf.clear());
        }
    }

    @State(Scope.Benchmark)
    public static class TestState {
        private long[] valuesToWrite = ThreadLocalRandom.current().longs(NUMBER_OF_VALUES_TO_WRITE, -MAX_VALUE, MAX_VALUE).toArray();
        private ByteBuf byteBuf = Unpooled.buffer(BUF_LENGTH, BUF_LENGTH);
    }
}
