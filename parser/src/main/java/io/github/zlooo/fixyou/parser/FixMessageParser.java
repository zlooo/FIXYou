package io.github.zlooo.fixyou.parser;

import com.carrotsearch.hppcrt.IntByteMap;
import com.carrotsearch.hppcrt.IntObjectMap;
import com.carrotsearch.hppcrt.maps.IntByteHashMap;
import com.carrotsearch.hppcrt.maps.IntObjectHashMap;
import io.github.zlooo.fixyou.Closeable;
import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@FieldNameConstants(level = AccessLevel.PRIVATE)
public class FixMessageParser implements Resettable, Closeable {

    static final byte NO_VALUE = (byte) -2;
    static final byte GROUP_CONSTITUENTS_INITIAL_VALUE = (byte) -1;


    @Getter
    private final ByteBufComposer bytesToParse;
    @Getter
    private final FixMessage fixMessage;
    private final FixMessageParserByteProcessor byteProcessor;

    public FixMessageParser(ByteBufComposer bytesToParse, FixSpec fixSpec, FixMessage fixMessage) {
        this.bytesToParse = bytesToParse;
        this.fixMessage = fixMessage;
        final int[] bodyFieldsOrder = fixSpec.getBodyFieldsOrder();
        final int numberOfBodyFields = bodyFieldsOrder.length;
        final int[] headerFieldsOrder = fixSpec.getHeaderFieldsOrder();
        final FieldType[] headerFieldTypes = fixSpec.getHeaderFieldTypes();
        final int numberOfHeaderFields = headerFieldsOrder.length;
        final IntObjectMap<FieldType> numberToFieldType = new IntObjectHashMap<>(numberOfBodyFields + numberOfHeaderFields);
        for (int i = 0; i < numberOfHeaderFields; i++) {
            numberToFieldType.put(ArrayUtils.getElementAt(headerFieldsOrder, i), ArrayUtils.getElementAt(headerFieldTypes, i));
        }
        final FieldType[] fieldTypes = fixSpec.getBodyFieldTypes();
        final IntObjectMap<IntByteMap> groupFieldsConstituents = new IntObjectHashMap<>();
        for (int i = 0; i < numberOfBodyFields; i++) {
            final int fieldNumber = ArrayUtils.getElementAt(bodyFieldsOrder, i);
            numberToFieldType.put(fieldNumber, ArrayUtils.getElementAt(fieldTypes, i));
            final FixSpec.FieldNumberType[] groupConstituents = fixSpec.getRepeatingGroupFieldNumbers(fieldNumber);
            final int repeatingGroupSize = groupConstituents.length;
            if (repeatingGroupSize > 0) {
                final IntByteMap groupFieldConstituents = new IntByteHashMap(repeatingGroupSize);
                groupFieldConstituents.setDefaultValue(NO_VALUE);
                for (int j = 0; j < repeatingGroupSize; j++) {
                    final FixSpec.FieldNumberType fieldNumberType = ArrayUtils.getElementAt(groupConstituents, j);
                    final int groupConstituentNumber = fieldNumberType.getNumber();
                    groupFieldConstituents.put(groupConstituentNumber, GROUP_CONSTITUENTS_INITIAL_VALUE);
                    numberToFieldType.put(groupConstituentNumber, fieldNumberType.getType());
                }
                groupFieldsConstituents.put(fieldNumber, groupFieldConstituents);
            }
        }
        this.byteProcessor = new FixMessageParserByteProcessor(fixMessage, numberToFieldType, groupFieldsConstituents);
    }

    @Override
    public void close() {
        byteProcessor.close();
    }

    @Override
    public void reset() {
        fixMessage.reset();
        byteProcessor.reset();
    }

    public void parseFixMsgBytes() {
        final int bytesRead = bytesToParse.forEachByte(byteProcessor);
        bytesToParse.readerIndex(bytesToParse.readerIndex() + bytesRead);
    }

    public boolean canContinueParsing() {
        return !bytesToParse.readerIndexBeyondStoredEnd();
    }

    public boolean isDone() {
        return fixMessage.isValueSet(FixConstants.CHECK_SUM_FIELD_NUMBER);
    }
}
