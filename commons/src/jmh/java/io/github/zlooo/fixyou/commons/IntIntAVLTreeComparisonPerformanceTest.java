package io.github.zlooo.fixyou.commons;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.ThreadLocalRandom;

/**
 * base - base
 * Benchmark                                                      Mode  Cnt    Score    Error  Units
 * IntIntAVLTreeComparisonPerformanceTest.avlTreeBaseFloorEntry  thrpt    5  832,534 ± 26,820  ops/s
 * IntIntAVLTreeComparisonPerformanceTest.avlTreeFloorEntry      thrpt    5  830,435 ± 23,336  ops/s
 * IntIntAVLTreeComparisonPerformanceTest.avlTreeBasePut         thrpt    5  937,329 ± 18,408  ops/s
 * IntIntAVLTreeComparisonPerformanceTest.avlTreePut             thrpt    5  818,642 ± 17,141  ops/s
 * IntIntAVLTreeComparisonPerformanceTest.avlTreeBaseRemove      thrpt    5  848,531 ± 35,416  ops/s
 * IntIntAVLTreeComparisonPerformanceTest.avlTreeRemove          thrpt    5  875,350 ± 26,257  ops/s
 *
 * base - after ArrayUtils instead of plain array operator
 * Benchmark                                                      Mode  Cnt    Score    Error  Units
 * IntIntAVLTreeComparisonPerformanceTest.avlTreeBaseFloorEntry  thrpt    5  845,353 ±  8,363  ops/s
 * IntIntAVLTreeComparisonPerformanceTest.avlTreeFloorEntry      thrpt    5  780,374 ±  5,272  ops/s
 * IntIntAVLTreeComparisonPerformanceTest.avlTreeBasePut         thrpt    5  947,630 ± 23,710  ops/s
 * IntIntAVLTreeComparisonPerformanceTest.avlTreePut             thrpt    5  759,924 ± 11,916  ops/s
 * IntIntAVLTreeComparisonPerformanceTest.avlTreeBaseRemove      thrpt    5  885,031 ± 11,277  ops/s
 * IntIntAVLTreeComparisonPerformanceTest.avlTreeRemove          thrpt    5  820,532 ±  7,433  ops/s
 */
@Fork(1)
public class IntIntAVLTreeComparisonPerformanceTest {

    private static final int SIZE = 10_000;

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void avlTreeBaseFloorEntry(TestState state, Blackhole blackhole) {
        for (final int key : state.keysToSearch) {
            blackhole.consume(state.avlTreeBase.floorValue(key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void avlTreeBasePut(TestState state, Blackhole blackhole) {
        for (final int key : state.keysToSearch) {
            blackhole.consume(state.emptyAvlTreeBase.put(key, key));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void avlTreeBaseRemove(TestState state, Blackhole blackhole) {
        for (final int key : state.keysToSearch) {
            blackhole.consume(state.avlTreeBase.remove(key));
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
        private final IntIntAVLTreeBase avlTreeBase = new IntIntAVLTreeBase(SIZE);
        private final IntIntAVLTreeBase emptyAvlTreeBase = new IntIntAVLTreeBase(SIZE);
        private final IntIntAVLTreeArrayUtils avlTree = new IntIntAVLTreeArrayUtils(SIZE);
        private final IntIntAVLTreeArrayUtils emptyAvlTree = new IntIntAVLTreeArrayUtils(SIZE);
        private final int[] keysToSearch = new int[SIZE];

        @Setup
        public void setup() {
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < SIZE; i++) {
                final int key = random.nextInt(Integer.MAX_VALUE);
                final int value = random.nextInt(Integer.MAX_VALUE);
                avlTreeBase.put(key, value);
                avlTree.put(key, value);
                keysToSearch[i] = random.nextInt(Integer.MAX_VALUE);
            }
        }
    }
}
