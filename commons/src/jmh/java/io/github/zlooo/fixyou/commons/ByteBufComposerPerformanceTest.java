package io.github.zlooo.fixyou.commons;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ByteProcessor;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 27.02.2021
 * Benchmark                                                             Mode  Cnt        Score        Error  Units
 * ByteBufComposerPerformanceTest.indexOf                               thrpt    5  9146427,575 ± 461524,553  ops/s
 * ByteBufComposerPerformanceTest.byteProcessor                         thrpt    5  7901361,753 ± 455530,538  ops/s
 * ByteBufComposerPerformanceTest.findReaderComponentIndexNavigableMap  thrpt    5       78,579 ±      1,911  ops/s
 * ByteBufComposerPerformanceTest.findReaderComponentIndexBruteForce    thrpt    5        0,493 ±      0,020  ops/s
 */
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

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void findReaderComponentIndexBruteForce(TestState testState, Blackhole blackhole) {
        for (final int index : testState.indexes) {
            for (int i = 0; i < testState.components.length; i++) {
                final ByteBufComposer.Component component = ArrayUtils.getElementAt(testState.components, i);
                if (component.getEndIndex() >= index && component.getStartIndex() <= index) {
                    blackhole.consume(i);
                }
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void findReaderComponentIndexNavigableMap(TestState testState, Blackhole blackhole) {
        for (final int index : testState.indexes) {
            final Map.Entry<Integer, Integer> entry = testState.startToIndex.floorEntry(index);
            final Integer indexCandidate;
            if (entry != null) {
                indexCandidate = entry.getValue();
            } else {
                indexCandidate = testState.startToIndex.lastEntry().getValue();
            }
            if (ArrayUtils.getElementAt(testState.components, indexCandidate).getEndIndex() <= index) {
                blackhole.consume(indexCandidate);
            }
        }
    }

    @State(Scope.Benchmark)
    public static class TestState {
        private static final int COMPONENT_INDEX_RANGE = 1000;
        private static final int NUMBER_OF_INDEXES = 100_000;
        private ByteBuf buf = Unpooled.directBuffer(INITIAL_CAPACITY);
        private byte valueToFind = (byte) ThreadLocalRandom.current().nextInt();
        private ByteProcessor byteProcessor = new ByteProcessor.IndexOfProcessor(valueToFind);
        private ByteBufComposer.Component[] components = new ByteBufComposer.Component[DefaultConfiguration.BYTE_BUF_COMPOSER_DEFAULT_COMPONENT_NUMBER];
        private int[] indexes = new int[NUMBER_OF_INDEXES];
        private NavigableMap<Integer, Integer> startToIndex = new TreeMap<>();

        @Setup
        public void setup() {
            final byte[] data = new byte[INITIAL_CAPACITY];
            ThreadLocalRandom.current().nextBytes(data);
            buf.writeBytes(data);
            for (int i = 0; i < components.length; i++) {
                final ByteBufComposer.Component component = new ByteBufComposer.Component();
                component.setStartIndex(i * COMPONENT_INDEX_RANGE);
                component.setEndIndex((i + 1) * COMPONENT_INDEX_RANGE - 1);
                components[i] = component;
                startToIndex.put(component.getStartIndex(), i);
            }
            final int indexBound = components[components.length - 1].getEndIndex() + 1;
            for (int i = 0; i < indexes.length; i++) {
                indexes[i] = ThreadLocalRandom.current().nextInt(indexBound);
            }
        }
    }
}
