package io.github.zlooo.fixyou.commons.utils;

import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.util.internal.PlatformDependent;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Benchmark                                                         Mode  Cnt   Score   Error  Units
 * ArrayDataGetPerformanceTest.randomNormalByteGet                  thrpt    5   6,331 ± 0,066  ops/s
 * ArrayDataGetPerformanceTest.randomPlatformDependentByteGet       thrpt    5   6,393 ± 0,084  ops/s
 * ArrayDataGetPerformanceTest.randomUnsafeByteGet                  thrpt    5   6,222 ± 0,033  ops/s
 * ArrayDataGetPerformanceTest.randomNormalIntGet                   thrpt    5   5,098 ± 0,314  ops/s
 * ArrayDataGetPerformanceTest.randomPlatformDependentIntGet        thrpt    5   6,270 ± 0,067  ops/s
 * ArrayDataGetPerformanceTest.randomUnsafeIntGet                   thrpt    5   5,209 ± 0,018  ops/s
 * ArrayDataGetPerformanceTest.randomNormalReferenceGet             thrpt    5   4,385 ± 0,157  ops/s
 * ArrayDataGetPerformanceTest.randomUnsafeReferenceGet             thrpt    5   5,787 ± 0,027  ops/s
 * ArrayDataGetPerformanceTest.sequentialNormalForLoopByteGet       thrpt    5  47,856 ± 0,709  ops/s
 * ArrayDataGetPerformanceTest.sequentialNormalIndexedByteGet       thrpt    5  43,109 ± 0,177  ops/s
 * ArrayDataGetPerformanceTest.sequentialPlatformDependentByteGet   thrpt    5  43,652 ± 0,164  ops/s
 * ArrayDataGetPerformanceTest.sequentialUnsafeByteGet              thrpt    5  43,681 ± 0,191  ops/s
 * ArrayDataGetPerformanceTest.sequentialNormalForLoopIntGet        thrpt    5  47,866 ± 0,963  ops/s
 * ArrayDataGetPerformanceTest.sequentialNormalIndexedIntGet        thrpt    5  42,862 ± 0,105  ops/s
 * ArrayDataGetPerformanceTest.sequentialPlatformDependentIntGet    thrpt    5  42,160 ± 0,258  ops/s
 * ArrayDataGetPerformanceTest.sequentialUnsafeIntGet               thrpt    5  40,918 ± 0,818  ops/s
 * ArrayDataGetPerformanceTest.sequentialNormalForLoopReferenceGet  thrpt    5  38,549 ± 0,140  ops/s
 * ArrayDataGetPerformanceTest.sequentialNormalIndexedReferenceGet  thrpt    5  35,091 ± 0,176  ops/s
 * ArrayDataGetPerformanceTest.sequentialUnsafeReferenceGet         thrpt    5  35,373 ± 0,161  ops/s
 */
@Fork(1)
public class ArrayDataGetPerformanceTest {

    private static final int ARRAY_SIZE = 10_000_000;
    private static final int NUMBER_OF_GETS = 10_000_000;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void randomNormalByteGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(state.byteData[index]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void randomPlatformDependentByteGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(PlatformDependent.getByte(state.byteData, index));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void randomUnsafeByteGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(ArrayUtils.getElementAt(state.byteData, index));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void randomNormalIntGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(state.intData[index]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void randomPlatformDependentIntGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(PlatformDependent.getInt(state.intData, index));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void randomUnsafeIntGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(ArrayUtils.getElementAt(state.intData, index));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void randomNormalReferenceGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(state.objectData[index]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void randomUnsafeReferenceGet(TestState state, Blackhole blackhole) {
        for (final int index : state.indexesToGet) {
            blackhole.consume(ArrayUtils.getElementAt(state.objectData, index));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sequentialNormalForLoopIntGet(TestState state, Blackhole blackhole) {
        for (final int data : state.intData) {
            blackhole.consume(data);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sequentialNormalIndexedIntGet(TestState state, Blackhole blackhole) {
        for (int i = 0; i < state.intData.length; i++) {
            blackhole.consume(state.intData[i]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sequentialPlatformDependentIntGet(TestState state, Blackhole blackhole) {
        for (int i = 0; i < state.intData.length; i++) {
            blackhole.consume(PlatformDependent.getInt(state.intData, i));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sequentialUnsafeIntGet(TestState state, Blackhole blackhole) {
        for (int i = 0; i < state.intData.length; i++) {
            blackhole.consume(ArrayUtils.getElementAt(state.intData, i));
        }
    }


    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sequentialNormalIndexedByteGet(TestState state, Blackhole blackhole) {
        for (int i = 0; i < state.byteData.length; i++) {
            blackhole.consume(state.byteData[i]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sequentialNormalForLoopByteGet(TestState state, Blackhole blackhole) {
        for (final byte data : state.byteData) {
            blackhole.consume(data);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sequentialPlatformDependentByteGet(TestState state, Blackhole blackhole) {
        for (int i = 0; i < state.byteData.length; i++) {
            blackhole.consume(PlatformDependent.getByte(state.byteData, i));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sequentialUnsafeByteGet(TestState state, Blackhole blackhole) {
        for (int i = 0; i < state.byteData.length; i++) {
            blackhole.consume(ArrayUtils.getElementAt(state.byteData, i));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sequentialNormalForLoopReferenceGet(TestState state, Blackhole blackhole) {
        for (final Object object : state.objectData) {
            blackhole.consume(object);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sequentialNormalIndexedReferenceGet(TestState state, Blackhole blackhole) {
        for (int i = 0; i < state.objectData.length; i++) {
            blackhole.consume(state.objectData[i]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sequentialUnsafeReferenceGet(TestState state, Blackhole blackhole) {
        for (int i = 0; i < state.objectData.length; i++) {
            blackhole.consume(ArrayUtils.getElementAt(state.objectData, i));
        }
    }

    @State(Scope.Benchmark)
    public static class TestState {
        private final byte[] byteData = generateByteArray(ARRAY_SIZE);
        private final int[] intData = generateIntArray(ARRAY_SIZE);
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

        private static int[] generateIntArray(int arraySize) {
            return ThreadLocalRandom.current().ints(arraySize).toArray();
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
