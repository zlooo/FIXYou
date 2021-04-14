package io.github.zlooo.fixyou.model;

import com.carrotsearch.hppcrt.IntIntMap;
import com.carrotsearch.hppcrt.heaps.IntIndexedHeapPriorityQueue;
import com.carrotsearch.hppcrt.maps.IntIntHashMap;
import com.carrotsearch.hppcrt.sets.IntHashSet;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 08.04.2021
 * Benchmark                                                Mode  Cnt      Score     Error  Units
 * HppcrtPerformanceTest.indexedHeapPriorityQueueGet       thrpt    5  18633,673 ±  52,276  ops/s
 * HppcrtPerformanceTest.hashMapGet                        thrpt    5   9957,155 ±  30,710  ops/s
 * HppcrtPerformanceTest.hashMapPut                        thrpt    5  19565,460 ± 623,457  ops/s
 * HppcrtPerformanceTest.indexedHeapPriorityQueuePut       thrpt    5   4683,457 ± 156,023  ops/s
 * HppcrtPerformanceTest.indexedHeapPriorityQueueContains  thrpt    5  23085,262 ±  74,454  ops/s
 * HppcrtPerformanceTest.hashMapContains                   thrpt    5  11915,578 ±  68,309  ops/s
 * HppcrtPerformanceTest.hashSetContains                   thrpt    5  11909,563 ±  20,291  ops/s
 * HppcrtPerformanceTest.sortedArrayContains               thrpt    5   1682,619 ± 150,577  ops/s
 */
public class HppcrtPerformanceTest {

    private static final int NUMBER_OF_ENTRIES = 10_000;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void hashMapGet(ReadTestState testState, Blackhole blackhole) {
        for (final int key : testState.keys) {
            blackhole.consume(testState.hashMap.get(key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void indexedHeapPriorityQueueGet(ReadTestState testState, Blackhole blackhole) {
        for (final int key : testState.keys) {
            blackhole.consume(testState.indexedHeapPriorityQueue.get(key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void hashMapPut(WriteTestState testState) {
        for (int i = 0; i < testState.keys.length; i++) {
            testState.hashMap.put(testState.keys[i], testState.values[i]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void indexedHeapPriorityQueuePut(WriteTestState testState) {
        for (int i = 0; i < testState.keys.length; i++) {
            testState.indexedHeapPriorityQueue.put(testState.keys[i], testState.values[i]);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void hashSetContains(ReadTestState testState, Blackhole blackhole) {
        for (final int key : testState.keys) {
            blackhole.consume(testState.hashSet.contains(key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void hashMapContains(ReadTestState testState, Blackhole blackhole) {
        for (final int key : testState.keys) {
            blackhole.consume(testState.hashMap.containsKey(key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void indexedHeapPriorityQueueContains(ReadTestState testState, Blackhole blackhole) {
        for (final int key : testState.keys) {
            blackhole.consume(testState.indexedHeapPriorityQueue.containsKey(key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sortedArrayContains(ReadTestState testState, Blackhole blackhole) {
        for (final int key : testState.keys) {
            blackhole.consume(Arrays.binarySearch(testState.sortedArray, key) < 0);
        }
    }

    @State(Scope.Benchmark)
    public static class ReadTestState {
        private final IntHashSet hashSet = new IntHashSet();
        private final IntIntMap hashMap = new IntIntHashMap(NUMBER_OF_ENTRIES);
        private final IntIntMap indexedHeapPriorityQueue = new IntIndexedHeapPriorityQueue(NUMBER_OF_ENTRIES);
        private int[] sortedArray = new int[NUMBER_OF_ENTRIES];
        private int[] keys = new int[NUMBER_OF_ENTRIES];

        @Setup
        public void setup() {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < NUMBER_OF_ENTRIES; i++) {
                final int key = Math.abs(random.nextInt(NUMBER_OF_ENTRIES));
                final int value = random.nextInt();
                keys[i] = key;
                hashMap.put(key, value);
                indexedHeapPriorityQueue.put(key, value);
                hashSet.add(key);
                sortedArray[i] = key;
            }
            Arrays.sort(sortedArray);
        }
    }

    @State(Scope.Benchmark)
    public static class WriteTestState {
        private final IntIntMap hashMap = new IntIntHashMap(NUMBER_OF_ENTRIES);
        private final IntIntMap indexedHeapPriorityQueue = new IntIndexedHeapPriorityQueue(NUMBER_OF_ENTRIES);
        private int[] keys = new int[NUMBER_OF_ENTRIES];
        private int[] values = new int[NUMBER_OF_ENTRIES];

        @Setup
        public void setup() {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < NUMBER_OF_ENTRIES; i++) {
                keys[i] = Math.abs(random.nextInt(NUMBER_OF_ENTRIES));
                values[i] = random.nextInt();
            }
        }
    }
}
