package io.github.zlooo.fixyou.model;

import com.carrotsearch.hppcrt.IntByteMap;
import com.carrotsearch.hppcrt.IntObjectMap;
import com.carrotsearch.hppcrt.heaps.ByteIndexedHeapPriorityQueue;
import com.carrotsearch.hppcrt.heaps.ObjectIndexedHeapPriorityQueue;
import io.github.zlooo.fixyou.utils.ArrayUtils;

import javax.annotation.Nonnull;

public class DefaultExtendedFixSpec implements ExtendedFixSpec {

    private final FixSpec fixSpec;
    private final IntObjectMap<FieldType> fieldNumberToType;
    private final IntByteMap headerFieldNumbers;

    public DefaultExtendedFixSpec(FixSpec fixSpec) {
        this.fixSpec = fixSpec;
        final int[] fieldsOrder = fixSpec.getBodyFieldsOrder();
        final FieldType[] fieldTypes = fixSpec.getBodyFieldTypes();
        this.fieldNumberToType = new ObjectIndexedHeapPriorityQueue<>(fieldsOrder.length);
        for (int i = 0; i < fieldsOrder.length; i++) {
            fieldNumberToType.put(ArrayUtils.getElementAt(fieldsOrder, i), ArrayUtils.getElementAt(fieldTypes, i));
        }
        final int[] headerFieldsOrder = fixSpec.getHeaderFieldsOrder();
        this.headerFieldNumbers = new ByteIndexedHeapPriorityQueue(headerFieldsOrder.length);
        for (int i = 0; i < headerFieldsOrder.length; i++) {
            headerFieldNumbers.put(ArrayUtils.getElementAt(headerFieldsOrder, i), (byte) 0);
        }
    }

    @Override
    public IntObjectMap<FieldType> getFieldNumberToType() {
        return fieldNumberToType;
    }

    @Override
    public IntByteMap getHeaderFieldNumbers() {
        return headerFieldNumbers;
    }

    @Nonnull
    @Override
    public int[] getHeaderFieldsOrder() {
        return fixSpec.getHeaderFieldsOrder();
    }

    @Nonnull
    @Override
    public FieldType[] getHeaderFieldTypes() {
        return fixSpec.getHeaderFieldTypes();
    }

    @Nonnull
    @Override
    public int[] getBodyFieldsOrder() {
        return fixSpec.getBodyFieldsOrder();
    }

    @Nonnull
    @Override
    public FieldType[] getBodyFieldTypes() {
        return fixSpec.getBodyFieldTypes();
    }

    @Nonnull
    @Override
    public char[][] getMessageTypes() {
        return fixSpec.getMessageTypes();
    }

    @Nonnull
    @Override
    public ApplicationVersionID applicationVersionId() {
        return fixSpec.applicationVersionId();
    }

    @Nonnull
    @Override
    public FieldNumberType[] getRepeatingGroupFieldNumbers(int groupNumber) {
        return fixSpec.getRepeatingGroupFieldNumbers(groupNumber);
    }
}
