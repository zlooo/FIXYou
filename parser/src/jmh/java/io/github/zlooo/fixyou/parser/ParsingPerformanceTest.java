package io.github.zlooo.fixyou.parser;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;

public class ParsingPerformanceTest {

    private static final int LONG_FIELD_DATA_LENGTH = 8;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void parseLong(TestState testState, Blackhole blackhole) {
        testState.byteBufComposer.readerIndex(0);
        blackhole.consume(FieldValueParser.parseLong(testState.byteBufComposer, 0, io.github.zlooo.fixyou.model.FixMessage.FIELD_SEPARATOR));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void longFieldGetValue(TestState testState, Blackhole blackhole) {
        testState.byteBufComposer.readerIndex(0);
        final int length = testState.endIndex - testState.startIndex;
        FieldValueParser.readChars(testState.byteBufComposer, 0, length, testState.rawValue, testState.unparsedValue);
        final boolean negative = testState.unparsedValue[0] == '-';
        long value = 0;
        for (int i = negative ? 1 : 0; i < length; i++) {
            final char nextChar = testState.unparsedValue[i];
            value = value * FieldValueParser.RADIX + ((int) nextChar - AsciiCodes.ZERO);
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
        private ByteBufComposer byteBufComposer;
        private int startIndex;
        private int endIndex;
        private final ByteBuf rawValue = Unpooled.buffer(LONG_FIELD_DATA_LENGTH, LONG_FIELD_DATA_LENGTH);
        private final char[] unparsedValue = new char[LONG_FIELD_DATA_LENGTH];

        @Setup
        public void setup() {
            startIndex = 0;
            final ByteBuf byteBuf = Unpooled.buffer(LONG_FIELD_DATA_LENGTH, LONG_FIELD_DATA_LENGTH);
            byteBuf.writeCharSequence(valueToParse, StandardCharsets.US_ASCII);
            endIndex = byteBuf.writerIndex();
            byteBuf.writeByte(FixMessage.FIELD_SEPARATOR);
            byteBufComposer = new ByteBufComposer(1);
            byteBufComposer.addByteBuf(byteBuf);
        }
    }
}
