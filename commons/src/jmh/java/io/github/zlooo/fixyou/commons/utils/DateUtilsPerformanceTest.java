package io.github.zlooo.fixyou.commons.utils;

import io.github.zlooo.fixyou.FixConstants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import org.openjdk.jmh.annotations.*;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class DateUtilsPerformanceTest {

    private static final int BUF_LENGTH = 21;

    private ByteBuf byteBuf = Unpooled.buffer(BUF_LENGTH, BUF_LENGTH);
    private long valueToWrite;
    private Clock clock;

    @Setup
    public void setup() {
        valueToWrite = System.currentTimeMillis();
        clock = Clock.fixed(Instant.ofEpochMilli(valueToWrite), ZoneOffset.UTC);
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public void directWriteTest() {
        DateUtils.writeTimestamp(valueToWrite, byteBuf.clear(), true);
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public void writeViaDateFormatterTest() {
        byteBuf.clear();
        for (final char singleCharacter : FixConstants.UTC_TIMESTAMP_FORMATTER.format(OffsetDateTime.now(clock)).toCharArray()) {
            byteBuf.writeByte(AsciiString.c2b(singleCharacter));
        }
    }
}
