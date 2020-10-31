package io.github.zlooo.fixyou.parser.model;

import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.FixSpec50SP2;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@Fork(1)
public class FieldCodecPerformanceTest {

    private static final FixSpec FIX_50SP2_SPEC = new FixSpec50SP2();
    private static final int[] FIX_50SP2_FIELDS_ORDER = FIX_50SP2_SPEC.getFieldsOrder();
    private static final FieldCodec FIELD_CODEC = new FieldCodec();

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public void orderedArray(TestState state, Blackhole blackhole) {
        final Field[] fieldsOrdered = state.fieldsOrdered;
        for (int i = 0; i < fieldsOrdered.length; i++) {
            blackhole.consume(ArrayUtils.getElementAt(fieldsOrdered, i));
        }
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public void fieldsArray(TestState state, Blackhole blackhole) {
        final Field[] fields = state.fields;
        for (int i = 0; i < FIX_50SP2_FIELDS_ORDER.length; i++) {
            blackhole.consume(ArrayUtils.getElementAt(fields, ArrayUtils.getElementAt(FIX_50SP2_FIELDS_ORDER, i)));
        }
    }

    @State(Scope.Benchmark)
    public static class TestState {
        private Field[] fields = new Field[FIX_50SP2_SPEC.highestFieldNumber() + 1];
        private Field[] fieldsOrdered = new Field[FIX_50SP2_FIELDS_ORDER.length];

        @Setup
        public void setup() {
            for (int i = 0; i < FIX_50SP2_FIELDS_ORDER.length; i++) {
                final int fieldNumber = FIX_50SP2_FIELDS_ORDER[i];
                final Field field = new Field(fieldNumber, FIELD_CODEC);
                fields[fieldNumber] = field;
                fieldsOrdered[i] = field;
            }
        }

        @TearDown
        public void tearDown() {
            for (final Field field : fieldsOrdered) {
                field.close();
            }
        }
    }
}
