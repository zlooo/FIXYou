package io.github.zlooo.fixyou.netty.handler;

import com.carrotsearch.hppcrt.IntLongMap;
import com.carrotsearch.hppcrt.heaps.LongIndexedHeapPriorityQueue;
import com.carrotsearch.hppcrt.maps.IntLongHashMap;
import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.netty.utils.FixSpec50SP2;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.BitSet;

/**
 * 27.02.2021
 * Benchmark                                      Mode  Cnt       Score       Error  Units
 * MessageEncoderPerformanceTest.bitset          thrpt    5  787982,582 ±  5969,735  ops/s
 * MessageEncoderPerformanceTest.mapContainsKey  thrpt    5  235848,940 ± 12492,693  ops/s
 * MessageEncoderPerformanceTest.sortAndSearch   thrpt    5  105180,697 ±  8791,257  ops/s
 */
public class MessageEncoderPerformanceTest {

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void hashMapContainsKey(TestState testState, Blackhole blackhole) {
        final int[] fieldsOrder = testState.fixSpec.getBodyFieldsOrder();
        for (int i = 2; i < fieldsOrder.length - 1; i++) {
            final int fieldNumber = ArrayUtils.getElementAt(fieldsOrder, i);
            if (testState.hashMap.containsKey(fieldNumber)) {
                blackhole.consume(fieldNumber);
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void indexedHeapPriorityQueueContainsKey(TestState testState, Blackhole blackhole) {
        final int[] fieldsOrder = testState.fixSpec.getBodyFieldsOrder();
        for (int i = 2; i < fieldsOrder.length - 1; i++) {
            final int fieldNumber = ArrayUtils.getElementAt(fieldsOrder, i);
            if (testState.indexedHeapPriorityQueue.containsKey(fieldNumber)) {
                blackhole.consume(fieldNumber);
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void bitset(TestState testState, Blackhole blackhole) {
        final int[] fieldsOrder = testState.fixSpec.getBodyFieldsOrder();
        for (int i = 2; i < fieldsOrder.length - 1; i++) {
            final int fieldNumber = ArrayUtils.getElementAt(fieldsOrder, i);
            if (testState.bitSet.get(fieldNumber)) {
                blackhole.consume(fieldNumber);
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void sortAndSearch(TestState testState, Blackhole blackhole) {
        final int[] setFieldNumbers = testState.fieldNumbers;
        Arrays.sort(setFieldNumbers);
        final int[] orderedFieldsToEncode = new int[setFieldNumbers.length];
        ArrayUtils.putElementAt(orderedFieldsToEncode, 0, FixConstants.BEGIN_STRING_FIELD_NUMBER);
        ArrayUtils.putElementAt(orderedFieldsToEncode, 1, FixConstants.BODY_LENGTH_FIELD_NUMBER);
        final int[] fieldsOrder = testState.fixSpec.getBodyFieldsOrder();
        for (int i = 2, j = 2; i < fieldsOrder.length - 1; i++) {
            final int fieldNumber = ArrayUtils.getElementAt(fieldsOrder, i);
            if (Arrays.binarySearch(setFieldNumbers, fieldNumber) >= 0) {
                ArrayUtils.putElementAt(orderedFieldsToEncode, j++, fieldNumber);
            }
        }
        for (int i = 0; i < orderedFieldsToEncode.length; i++) {
            blackhole.consume(ArrayUtils.getElementAt(orderedFieldsToEncode, i));
        }
    }

    @State(Scope.Benchmark)
    public static class TestState {
        private int[] fieldNumbers;
        private FixSpec fixSpec = new FixSpec50SP2();
        private IntLongMap hashMap = new IntLongHashMap(DefaultConfiguration.INITIAL_FIELDS_IN_MSG_NUMBER);
        private IntLongMap indexedHeapPriorityQueue = new LongIndexedHeapPriorityQueue(DefaultConfiguration.INITIAL_FIELDS_IN_MSG_NUMBER);
        private BitSet bitSet = new BitSet(DefaultConfiguration.INITIAL_FIELDS_IN_MSG_NUMBER);

        @Setup
        public void setup() {
            fieldNumbers =
                    new int[]{FieldNumbers.CLORD_ID, FieldNumbers.MESSAGE_TYPE, FieldNumbers.ORDER_ID, FieldNumbers.EXEC_ID, FieldNumbers.EXEC_TYPE, FieldNumbers.ORD_STATUS, FieldNumbers.SYMBOL, FieldNumbers.SIDE, FieldNumbers.LEAVES_QTY,
                            FieldNumbers.CUM_QTY};
            for (final int fieldNumber : fieldNumbers) {
                hashMap.put(fieldNumber, 0L);
                indexedHeapPriorityQueue.put(fieldNumber, 0L);
                bitSet.set(fieldNumber);
            }
        }
    }

    private static final class FieldNumbers {
        private static final int CLORD_ID = 11;
        private static final int MESSAGE_TYPE = 35;
        private static final int ORDER_ID = 37;
        private static final int EXEC_ID = 17;
        private static final int EXEC_TYPE = 150;
        private static final int ORD_STATUS = 39;
        private static final int SYMBOL = 55;
        private static final int SIDE = 54;
        private static final int LEAVES_QTY = 151;
        private static final int CUM_QTY = 14;
    }
}
