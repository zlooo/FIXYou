package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.parser.FixSpec50SP2;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

public class FixMessagePerformanceTest {

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void fieldsIteration(TestState state, Blackhole blackhole) throws Exception {
        for (final Field field : state.fixMessage.getFields()) {
            blackhole.consume(field);
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void fieldsOrderedIteration(TestState state, Blackhole blackhole) throws Exception {
        for (final Field field : state.fixMessage.getFieldsOrdered()) {
            blackhole.consume(field);
        }
    }

    @State(Scope.Benchmark)
    public static class TestState {
        private FixMessage fixMessage = new FixMessage(new FixSpec50SP2(), new FieldCodec());
    }
}
