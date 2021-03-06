package io.github.zlooo.fixyou.commons.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class FieldUtilsPerformanceTest {

    private static final int BUF_LENGTH = 8;
    private static final long WORST_CASE_VALUE = 9999999;

    private ByteBuf byteBuf = Unpooled.buffer(BUF_LENGTH, BUF_LENGTH);

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public void directWriteTest() {
        FieldUtils.writeEncoded(WORST_CASE_VALUE, byteBuf.clear());
    }
}
