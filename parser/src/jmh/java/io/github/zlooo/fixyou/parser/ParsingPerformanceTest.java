package io.github.zlooo.fixyou.parser;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.utils.AsciiCodes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.AsciiString;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;

public class ParsingPerformanceTest {

    private static final int LONG_FIELD_DATA_LENGTH = 8;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void parseLongByteProcessor(TestState testState, Blackhole blackhole) {
        testState.valueToParsAsComposer.readerIndex(0);
        blackhole.consume(FieldValueParser.parseLong(testState.valueToParsAsByteBuf, testState.longHolder));
        testState.valueToParsAsByteBuf.readerIndex(0);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void parseLongFromComposer(TestState testState, Blackhole blackhole) {
        testState.valueToParsAsComposer.readerIndex(0);
        blackhole.consume(parseLong(testState.valueToParsAsComposer, 0, FixMessage.FIELD_SEPARATOR));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void longFieldGetValue(TestState testState, Blackhole blackhole) {
        testState.valueToParsAsComposer.readerIndex(0);
        final int length = testState.endIndex - testState.startIndex;
        readChars(testState.valueToParsAsComposer, 0, length, testState.rawValue, testState.unparsedValue, testState.intHolder);
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

    private static void readChars(ByteBufComposer source, int srcIndex, int length, ByteBuf tempBuffer, char[] destination, ValueHolders.IntHolder counter) {
        tempBuffer.clear();
        source.getBytes(srcIndex, length, tempBuffer);
        counter.reset();
        tempBuffer.forEachByte(0, length, byteRead -> {
            destination[counter.getAndIncrement()] = AsciiString.b2c(byteRead);
            return true;
        });
    }

   private static long parseLong(ByteBufComposer byteBuf, int srcIndex, byte endIndicator) {
        long num = 0;
        boolean negative = false;
        int index = srcIndex;
        while (true) {
            final byte b = byteBuf.getByte(index++);
            if (b >= AsciiCodes.ZERO && b <= AsciiCodes.NINE) {
                num = num * FieldValueParser.RADIX + b - AsciiCodes.ZERO;
            } else if (b == AsciiCodes.MINUS) {
                negative = true;
            } else if (b == endIndicator) {
                break;
            }
        }
        return negative ? -num : num;
    }

    @State(Scope.Benchmark)
    public static class TestState {
        @Param({"-99999", "1234", "99999"})
        private String valueToParse;
        private ByteBufComposer valueToParsAsComposer;
        private ByteBuf valueToParsAsByteBuf;
        private int startIndex;
        private int endIndex;
        private final ByteBuf rawValue = Unpooled.buffer(LONG_FIELD_DATA_LENGTH, LONG_FIELD_DATA_LENGTH);
        private final ValueHolders.IntHolder intHolder = new ValueHolders.IntHolder();
        private final ValueHolders.LongHolder longHolder = new ValueHolders.LongHolder();

        private final char[] unparsedValue = new char[LONG_FIELD_DATA_LENGTH];
        @Setup
        public void setup() {
            startIndex = 0;
            final ByteBuf byteBuf = Unpooled.buffer(LONG_FIELD_DATA_LENGTH, LONG_FIELD_DATA_LENGTH);
            byteBuf.writeCharSequence(valueToParse, StandardCharsets.US_ASCII);
            valueToParsAsByteBuf = Unpooled.directBuffer();
            valueToParsAsByteBuf.writeBytes(byteBuf);
            byteBuf.readerIndex(0);
            endIndex = byteBuf.writerIndex();
            byteBuf.writeByte(FixMessage.FIELD_SEPARATOR);
            valueToParsAsComposer = new ByteBufComposer(1);
            valueToParsAsComposer.addByteBuf(byteBuf);
        }

    }
}
