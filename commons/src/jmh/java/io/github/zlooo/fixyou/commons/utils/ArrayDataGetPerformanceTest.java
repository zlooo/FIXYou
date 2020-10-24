package io.github.zlooo.fixyou.commons.utils;

import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.util.internal.PlatformDependent;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;

@Fork(1)
public class ArrayDataGetPerformanceTest {

    private static final int ARRAY_SIZE = 10_000_000;
    private static final int NUMBER_OF_GETS = 1_000_000;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void normalByteGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(state.byteData[index]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void platformDependentByteGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(PlatformDependent.getByte(state.byteData, index));
        }
    }


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void normalReferenceGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(state.objectData[index]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void unsafeReferenceGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(ArrayUtils.getElementAt(state.objectData, index));
        }
    }

    @State(Scope.Benchmark)
    public static class TestState {
        private final byte[] byteData = generateByteArray(ARRAY_SIZE);
        private final Object[] objectData = generateObjectArray(ARRAY_SIZE);
        private final int[] indexesToGet = generateIndexesToGet(NUMBER_OF_GETS, ARRAY_SIZE - 1);

        private int[] generateIndexesToGet(int numberOfGets, int maxIndex) {
            final int[] result = new int[numberOfGets];
            for (int i = 0; i < result.length; i++) {
                result[i] = ThreadLocalRandom.current().nextInt(0, maxIndex);
            }
            return result;
        }

        private static byte[] generateByteArray(int arraySize) {
            final byte[] result = new byte[arraySize];
            ThreadLocalRandom.current().nextBytes(result);
            return result;
        }

        private static Object[] generateObjectArray(int arraySize) {
            final Object[] result = new Object[arraySize];
            for (int i = 0; i < arraySize; i++) {
                result[i] = new Object();
            }
            return result;
        }
    }
}
