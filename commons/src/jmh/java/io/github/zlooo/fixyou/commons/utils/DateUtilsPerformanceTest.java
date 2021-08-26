package io.github.zlooo.fixyou.commons.utils;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.commons.ReusableCharArray;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark                                            Mode  Cnt  Score   Error   Units
 * DateUtilsPerformanceTest.directWriteTest            thrpt    5  3,705 ± 0,028  ops/us
 * DateUtilsPerformanceTest.writeViaDateFormatterTest  thrpt    5  2,214 ± 0,005  ops/us
 * DateUtilsPerformanceTest.parseViaDateUtilsTest      thrpt    5  8,030 ± 0,057  ops/us
 * DateUtilsPerformanceTest.parseViaDateFormatterTest  thrpt    5  1,464 ± 0,010  ops/us
 */
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class DateUtilsPerformanceTest {

    private static final int BUF_LENGTH = 21;
    private static final DateTimeFormatter UTC_TIMESTAMP_NO_MILLIS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS");

    private final ByteBuf byteBuf = Unpooled.buffer(BUF_LENGTH, BUF_LENGTH);
    private final char[] tempCharBuffer = new char[BUF_LENGTH];
    private final ByteBufComposer valueToParseAsComposer = new ByteBufComposer(1);
    private final ByteBuf valueToParseAsByteBuf = Unpooled.directBuffer();
    private final ReusableCharArray reusableCharArray = new ReusableCharArray();
    private final DateUtils.TimestampParser timestampParser = new DateUtils.TimestampParser();
    private long valueToWrite;
    private Clock clock;

    @Setup
    public void setup() {
        valueToWrite = System.currentTimeMillis();
        final ByteBuf bufferToParse = Unpooled.directBuffer(BUF_LENGTH);
        bufferToParse.writeCharSequence(UTC_TIMESTAMP_NO_MILLIS_FORMATTER.format(Instant.ofEpochMilli(valueToWrite).atZone(ZoneOffset.UTC)), StandardCharsets.US_ASCII);
        valueToParseAsByteBuf.writeBytes(bufferToParse);
        bufferToParse.readerIndex(0);
        valueToParseAsComposer.addByteBuf(bufferToParse);
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
        for (final char singleCharacter : UTC_TIMESTAMP_NO_MILLIS_FORMATTER.format(OffsetDateTime.now(clock)).toCharArray()) {
            byteBuf.writeByte(AsciiString.c2b(singleCharacter));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void parseViaDateFormatterTest(Blackhole blackhole) {
        readChars(valueToParseAsComposer, 0, BUF_LENGTH, byteBuf, tempCharBuffer);
        reusableCharArray.setCharArray(tempCharBuffer);
        blackhole.consume(UTC_TIMESTAMP_NO_MILLIS_FORMATTER.parse(reusableCharArray, LocalDateTime::from).toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void parseViaDateUtilsTest(Blackhole blackhole) {
        timestampParser.reset();
        blackhole.consume(DateUtils.parseTimestamp(valueToParseAsByteBuf, timestampParser));
        valueToParseAsByteBuf.readerIndex(0);
    }

    private static void readChars(ByteBufComposer source, int srcIndex, int length, ByteBuf tempBuffer, char[] destination) {
        tempBuffer.clear();
        source.getBytes(srcIndex, length, tempBuffer);
        for (int i = 0; i < length; i++) {
            destination[i] = AsciiString.b2c(tempBuffer.getByte(i));
        }
    }
}
