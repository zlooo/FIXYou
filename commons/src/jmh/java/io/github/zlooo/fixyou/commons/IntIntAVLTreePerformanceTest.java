package io.github.zlooo.fixyou.commons;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 08.03.2021
 * Benchmark                                        Mode  Cnt    Score    Error  Units
 * IntIntAVLTreePerformanceTest.avlTreeFloorEntry  thrpt    5  825,045 ±  2,588  ops/s
 * IntIntAVLTreePerformanceTest.treeMapFloorEntry  thrpt    5  794,667 ±  2,326  ops/s
 * IntIntAVLTreePerformanceTest.avlTreePut         thrpt    5  943,049 ±  8,582  ops/s
 * IntIntAVLTreePerformanceTest.treeMapPut         thrpt    5  789,180 ± 24,207  ops/s
 * IntIntAVLTreePerformanceTest.avlTreeRemove      thrpt    5  890,199 ±  3,554  ops/s
 * IntIntAVLTreePerformanceTest.treeMapRemove      thrpt    5  812,423 ± 14,006  ops/s
 */
@Fork(1)
public class IntIntAVLTreePerformanceTest {

    private static final int SIZE = 10_000;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void treeMapFloorEntry(TestState state, Blackhole blackhole) {
        for (final int key : state.keysToSearch) {
            blackhole.consume(state.treeMap.floorEntry(key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void treeMapPut(TestState state, Blackhole blackhole) {
        for (final int key : state.keysToSearch) {
            blackhole.consume(state.emptyTreeMap.put(key, key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void treeMapRemove(TestState state, Blackhole blackhole) {
        for (final int key : state.keysToSearch) {
            blackhole.consume(state.treeMap.remove(key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void avlTreeFloorEntry(TestState state, Blackhole blackhole) {
        for (final int key : state.keysToSearch) {
            blackhole.consume(state.avlTree.floorValue(key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void avlTreePut(TestState state, Blackhole blackhole) {
        for (final int key : state.keysToSearch) {
            blackhole.consume(state.emptyAvlTree.put(key, key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void avlTreeRemove(TestState state, Blackhole blackhole) {
        for (final int key : state.keysToSearch) {
            blackhole.consume(state.avlTree.remove(key));
        }
    }

    @State(Scope.Benchmark)
    public static class TestState {
        private final NavigableMap<Integer, Integer> treeMap = new TreeMap<>();
        private final NavigableMap<Integer, Integer> emptyTreeMap = new TreeMap<>();
        private final IntIntAVLTree avlTree = new IntIntAVLTree(SIZE);
        private final IntIntAVLTree emptyAvlTree = new IntIntAVLTree(SIZE);
        private final int[] keysToSearch = new int[SIZE];

        @Setup
        public void setup() {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < SIZE; i++) {
                final int key = random.nextInt(Integer.MAX_VALUE);
                final int value = random.nextInt(Integer.MAX_VALUE);
                treeMap.put(key, value);
                avlTree.put(key, value);
                keysToSearch[i] = random.nextInt(Integer.MAX_VALUE);
            }
        }
    }
}
