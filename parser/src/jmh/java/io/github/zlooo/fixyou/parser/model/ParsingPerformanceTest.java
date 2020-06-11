package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;

public class ParsingPerformanceTest {

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void parsingUtilsParseLong(TestState testState, Blackhole blackhole) {
        blackhole.consume(ParsingUtils.parseLong(testState.byteBuf.readerIndex(0), FixMessage.FIELD_SEPARATOR));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void longFieldGetValue(TestState testState, Blackhole blackhole) {
        testState.byteBuf.readerIndex(0);
        final int length = testState.endIndex - testState.startIndex;
        ParsingUtils.readChars(testState.byteBuf, length, testState.rawValue, testState.unparsedValue);
        final boolean negative = testState.unparsedValue[0] == '-';
        long value = 0;
        for (int i = negative ? 1 : 0; i < length; i++) {
            final char nextChar = testState.unparsedValue[i];
            value = value * ParsingUtils.RADIX + ((int) nextChar - AsciiCodes.ZERO);
        }
        if (negative) {
            value *= -1;
        }
        blackhole.consume(value);
    }

    @State(Scope.Benchmark)
    public static class TestState {
        @Param({"-99999", "1234", "99999"})
        private String valueToParse;
        private ByteBuf byteBuf;
        private int startIndex;
        private int endIndex;
        private final byte[] rawValue = new byte[LongField.FIELD_DATA_LENGTH];
        private final char[] unparsedValue = new char[LongField.FIELD_DATA_LENGTH];

        @Setup
        public void setup() {
            startIndex = 0;
            byteBuf = Unpooled.buffer(LongField.FIELD_DATA_LENGTH, LongField.FIELD_DATA_LENGTH);
            byteBuf.writeCharSequence(valueToParse, StandardCharsets.US_ASCII);
            endIndex = byteBuf.writerIndex();
            byteBuf.writeByte(FixMessage.FIELD_SEPARATOR);
        }
    }
}
