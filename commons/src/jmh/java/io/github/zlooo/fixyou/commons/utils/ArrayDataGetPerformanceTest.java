package io.github.zlooo.fixyou.commons.utils;

import io.netty.util.internal.PlatformDependent;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;

@Fork(1)
@State(Scope.Benchmark)
public class ArrayDataGetPerformanceTest {

    private static final int ARRAY_SIZE = 10_000_000;
    private static final int NUMBER_OF_GETS = 1_000_000;
    private final byte[] data = generateArray(ARRAY_SIZE);
    private final int[] indexesToGet = generateIndexesToGet(NUMBER_OF_GETS, ARRAY_SIZE - 1);

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void normalGet(Blackhole blackhole) {
        for (final int index : indexesToGet) {
            blackhole.consume(data[index]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void platformDependentGet(Blackhole blackhole) {
        for (final int index : indexesToGet) {
            blackhole.consume(PlatformDependent.getByte(data, index));
        }
    }

    private int[] generateIndexesToGet(int numberOfGets, int maxIndex) {
        final int[] result = new int[numberOfGets];
        for (int i = 0; i < result.length; i++) {
            result[i] = ThreadLocalRandom.current().nextInt(0, maxIndex);
        }
        return result;
    }

    private static byte[] generateArray(int arraySize) {
        final byte[] result = new byte[arraySize];
        ThreadLocalRandom.current().nextBytes(result);
        return result;
    }
}
