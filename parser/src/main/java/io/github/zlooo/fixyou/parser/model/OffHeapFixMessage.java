package io.github.zlooo.fixyou.parser.model;

import com.carrotsearch.hppcrt.IntCollection;
import com.carrotsearch.hppcrt.IntLongMap;
import com.carrotsearch.hppcrt.LongLongMap;
import com.carrotsearch.hppcrt.cursors.IntLongCursor;
import com.carrotsearch.hppcrt.cursors.LongLongCursor;
import com.carrotsearch.hppcrt.maps.IntLongHashMap;
import com.carrotsearch.hppcrt.maps.LongLongHashMap;
import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.FIXYouException;
import io.github.zlooo.fixyou.commons.AbstractPoolableFixMessage;
import io.github.zlooo.fixyou.commons.memory.MemoryConstants;
import io.github.zlooo.fixyou.commons.memory.Region;
import io.github.zlooo.fixyou.commons.pool.ObjectPool;
import io.github.zlooo.fixyou.commons.utils.ReflectionUtils;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.StandardHeaderAndFooterOnlyFixSpec;
import io.github.zlooo.fixyou.parser.ValueHolders;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.github.zlooo.fixyou.utils.UnsafeAccessor;
import io.netty.buffer.ByteBuf;
import io.netty.util.AsciiString;
import lombok.experimental.FieldNameConstants;
import sun.misc.Unsafe;

import javax.annotation.Nonnull;
import java.util.BitSet;

/**
 * Represents, surprise, surprise, fix message, I bet you did not see that coming ;)<br>
 * <br>
 * <h3>Limitations</h3>
 * <p><ul>
 * <li>Nested repeating groups - only 1 level of nesting is supported for now</li>
 * <li>Nested repeating groups should repeat the same number of times in each parent group repetition. For example let's take following message fragment 85=2 787=C 781=2 782=ID1 782=ID2 787=D 781=2 782=ID3 782=ID4. Note that group 781
 * which is nested in 85 has 2 repetitions in both 85 group repetitions</li>
 * </ul><p>
 */
@FieldNameConstants
public class OffHeapFixMessage extends AbstractPoolableFixMessage<OffHeapFixMessage> {

    private static final Unsafe UNSAFE = UnsafeAccessor.UNSAFE;
    private static final long NO_VALUE = -1;
    private static final String NO_REGIONS_IN_POOL_MSG_TEMPLATE = "No regions available in pool ";
    private static final String REGION_SIZE_ERROR_MSG_TEMPLATE = "Field size cannot be greater than region size! Requested field size %d, region size %d";

    private final ObjectPool<Region> regionPool;
    private final Region[] regions = new Region[DefaultConfiguration.INITIAL_REGION_ARRAY_SIZE];
    private final int regionSize;
    private int regionsIndex;
    private final IntLongMap fieldNumberToAddress = new IntLongHashMap(DefaultConfiguration.INITIAL_FIELDS_IN_MSG_NUMBER);
    private final BitSet fieldsSet = new BitSet(DefaultConfiguration.INITIAL_FIELDS_IN_MSG_NUMBER);
    private final LongLongMap repeatingGroupAddresses = new LongLongHashMap();
    private final DirectCharSequence directCharSequence = new DirectCharSequence();
    private int bodyLength;

    public OffHeapFixMessage(ObjectPool<Region> regionPool) {
        this.regionPool = regionPool;
        fieldNumberToAddress.setDefaultValue(NO_VALUE);
        repeatingGroupAddresses.setDefaultValue(NO_VALUE);
        final Region region = regionPool.tryGetAndRetain();
        if (region == null) {
            throw new FIXYouException("Region pool has run out of objects to create this fix message, please increase it");
        }
        regionSize = region.getSize();
        ArrayUtils.putElementAt(regions, 0, region);
    }

    @Override
    public boolean getBooleanValue(int fieldNumber) {
        return UNSAFE.getByte(fieldNumberToAddress.get(fieldNumber)) == 1;
    }

    @Override
    public boolean getBooleanValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return UNSAFE.getByte(repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber))) == 1;
    }

    @Override
    public void setBooleanValue(int fieldNumber, boolean newValue) {
        UNSAFE.putByte(fieldAddress(fieldNumber, FieldConstants.BOOLEAN_FIELD_SIZE), newValue ? (byte) 1 : (byte) 0);
        fieldsSet.set(fieldNumber);
    }

    @Override
    public void setBooleanValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, boolean newValue) {
        final boolean newRepeatingGroup = saveIndexIfGreater(groupNumber, repetitionIndex);
        UNSAFE.putByte(fieldAddress(fieldNumber, groupNumber, repetitionIndex, parentRepetitionIndex, FieldConstants.BOOLEAN_FIELD_SIZE), newValue ? (byte) 1 : (byte) 0);
        if (newRepeatingGroup) {
            fieldsSet.set(groupNumber);
        }
    }

    @Override
    public char getCharValue(int fieldNumber) {
        return UNSAFE.getChar(fieldNumberToAddress.get(fieldNumber));
    }

    @Override
    public char getCharValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return UNSAFE.getChar(repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)));
    }

    @Override
    public void setCharValue(int fieldNumber, char newValue) {
        UNSAFE.putChar(fieldAddress(fieldNumber, FieldConstants.CHAR_FIELD_SIZE), newValue);
        fieldsSet.set(fieldNumber);
    }

    @Override
    public void setCharValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char newValue) {
        final boolean newRepeatingGroup = saveIndexIfGreater(groupNumber, repetitionIndex);
        UNSAFE.putChar(fieldAddress(fieldNumber, groupNumber, repetitionIndex, parentRepetitionIndex, FieldConstants.CHAR_FIELD_SIZE), newValue);
        if (newRepeatingGroup) {
            fieldsSet.set(groupNumber);
        }
    }

    /**
     * Returns value as {@link CharSequence}.
     * However, remember that returned {@link CharSequence} is just a view, actual data is stored in one of this message's {@link Region}. So if you get this {@link CharSequence} and then return
     * {@link OffHeapFixMessage} object to pool you might and probably will get weird data as this {@link CharSequence} will point to region of memory that could be already used by different {@link OffHeapFixMessage} storing different
     * data, possibly
     * even different type. Same if you want to reuse this {@link OffHeapFixMessage} after calling {@link #reset()}, ie get some field's data, do reset and store this data again in clean message.
     * Also note this {@link CharSequence} is shared for whole message. Subsequent calls of this method will return the same instance of {@link CharSequence}, ie {@link #getCharSequenceValue(int)} == {@link #getCharSequenceValue(int)}
     *
     * @param fieldNumber you want to get data for
     * @return field's data as {@link CharSequence}
     */
    @Override
    public CharSequence getCharSequenceValue(int fieldNumber) {
        directCharSequence.startAddress = fieldNumberToAddress.get(fieldNumber);
        return directCharSequence;
    }

    /**
     * Variant of {@link #getCharSequenceValue(int)} that gets data from a field that's part of repeating group.
     *
     * @param fieldNumber     you want to get data for
     * @param repetitionIndex index of a repetition that you want to get data for, 0 based
     * @return field's data as {@link CharSequence}
     * @see #getCharSequenceValue(int)
     */
    @Override
    public CharSequence getCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        directCharSequence.startAddress = repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber));
        return directCharSequence;
    }

    @Override
    public char getCharSequenceLength(int fieldNumber) {
        return UNSAFE.getChar(fieldNumberToAddress.get(fieldNumber));
    }

    @Override
    public char getCharSequenceLength(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return UNSAFE.getChar(repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)));
    }

    @Override
    public void setCharSequenceValue(int fieldNumber, ByteBuf asciiByteBuffer) {
        final int numberOfChars = asciiByteBuffer.readableBytes();
        final long fieldAddress = fieldAddressWithLengthCheck(fieldNumber, (short) (numberOfChars * MemoryConstants.CHAR_SIZE + FieldConstants.CHAR_SEQUENCE_LENGTH_SIZE));
        doSetCharSequenceData(asciiByteBuffer, numberOfChars, fieldAddress);
        fieldsSet.set(fieldNumber);
    }

    @Override
    public void setCharSequenceValue(int fieldNumber, CharSequence newValue) {
        final int numberOfChars = newValue.length();
        final long fieldAddress = fieldAddressWithLengthCheck(fieldNumber, (short) (numberOfChars * MemoryConstants.CHAR_SIZE + FieldConstants.CHAR_SEQUENCE_LENGTH_SIZE));
        doSetCharSequenceData(newValue, numberOfChars, fieldAddress);
        fieldsSet.set(fieldNumber);
    }

    @Override
    public void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, ByteBuf asciiByteBuffer) {
        final int numberOfChars = asciiByteBuffer.readableBytes();
        final boolean newRepeatingGroup = saveIndexIfGreater(groupNumber, repetitionIndex);
        final long fieldAddress = fieldAddressWithLengthCheck(fieldNumber, groupNumber, repetitionIndex, parentRepetitionIndex, (short) (numberOfChars * MemoryConstants.CHAR_SIZE + FieldConstants.CHAR_SEQUENCE_LENGTH_SIZE));
        doSetCharSequenceData(asciiByteBuffer, numberOfChars, fieldAddress);
        if (newRepeatingGroup) {
            fieldsSet.set(groupNumber);
        }
    }

    @Override
    public void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, CharSequence newValue) {
        final int numberOfChars = newValue.length();
        final boolean newRepeatingGroup = saveIndexIfGreater(groupNumber, repetitionIndex);
        final long fieldAddress = fieldAddressWithLengthCheck(fieldNumber, groupNumber, repetitionIndex, parentRepetitionIndex, (short) (numberOfChars * MemoryConstants.CHAR_SIZE + FieldConstants.CHAR_SEQUENCE_LENGTH_SIZE));
        doSetCharSequenceData(newValue, numberOfChars, fieldAddress);
        if (newRepeatingGroup) {
            fieldsSet.set(groupNumber);
        }
    }

    @Override
    public void setCharSequenceValue(int fieldNumber, char[] newValue) {
        setCharSequenceValue(fieldNumber, newValue, newValue.length);
    }

    @Override
    public void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char[] newValue) {
        setCharSequenceValue(fieldNumber, groupNumber, repetitionIndex, parentRepetitionIndex, newValue, newValue.length);
    }

    @Override
    public void setCharSequenceValue(int fieldNumber, char[] newValue, int newValueLength) {
        final long fieldAddress = fieldAddressWithLengthCheck(fieldNumber, (short) (newValueLength * MemoryConstants.CHAR_SIZE + FieldConstants.CHAR_SEQUENCE_LENGTH_SIZE));
        doSetCharSequenceData(newValue, newValueLength, fieldAddress);
        fieldsSet.set(fieldNumber);
    }

    @Override
    public void setCharSequenceValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, char[] newValue, int newValueLength) {
        final boolean newRepeatingGroup = saveIndexIfGreater(groupNumber, repetitionIndex);
        final long fieldAddress = fieldAddressWithLengthCheck(fieldNumber, groupNumber, repetitionIndex, parentRepetitionIndex, (short) (newValueLength * MemoryConstants.CHAR_SIZE + FieldConstants.CHAR_SEQUENCE_LENGTH_SIZE));
        doSetCharSequenceData(newValue, newValueLength, fieldAddress);
        if (newRepeatingGroup) {
            fieldsSet.set(groupNumber);
        }
    }

    @Override
    public long getDoubleUnscaledValue(int fieldNumber) {
        return UNSAFE.getLong(fieldNumberToAddress.get(fieldNumber));
    }

    @Override
    public long getDoubleUnscaledValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return UNSAFE.getLong(repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)));
    }

    @Override
    public short getScale(int fieldNumber) {
        return UNSAFE.getShort(fieldNumberToAddress.get(fieldNumber) + MemoryConstants.LONG_SIZE);
    }

    @Override
    public short getScale(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return UNSAFE.getShort(repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)) + MemoryConstants.LONG_SIZE);
    }

    @Override
    public void setDoubleValue(int fieldNumber, long newValue, short newScale) {
        final long fieldAddress = fieldAddress(fieldNumber, FieldConstants.DOUBLE_FIELD_SIZE);
        doSetDoubleValue(newValue, newScale, fieldAddress);
        fieldsSet.set(fieldNumber);
    }

    @Override
    public void setDoubleValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue, short newScale) {
        final boolean newRepeatingGroup = saveIndexIfGreater(groupNumber, repetitionIndex);
        final long fieldAddress = fieldAddress(fieldNumber, groupNumber, repetitionIndex, parentRepetitionIndex, FieldConstants.DOUBLE_FIELD_SIZE);
        doSetDoubleValue(newValue, newScale, fieldAddress);
        if (newRepeatingGroup) {
            fieldsSet.set(groupNumber);
        }
    }

    @Override
    public long getLongValue(int fieldNumber) {
        return UNSAFE.getLong(fieldNumberToAddress.get(fieldNumber));
    }

    @Override
    public long getLongValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return UNSAFE.getLong(repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)));
    }

    @Override
    public void setLongValue(int fieldNumber, long newValue) {
        UNSAFE.putLong(fieldAddress(fieldNumber, MemoryConstants.LONG_SIZE), newValue);
        fieldsSet.set(fieldNumber);
    }

    @Override
    public void setLongValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue) {
        final boolean newRepeatingGroup = saveIndexIfGreater(groupNumber, repetitionIndex);
        UNSAFE.putLong(fieldAddress(fieldNumber, groupNumber, repetitionIndex, parentRepetitionIndex, MemoryConstants.LONG_SIZE), newValue);
        if (newRepeatingGroup) {
            fieldsSet.set(groupNumber);
        }
    }

    @Override
    public long getTimestampValue(int fieldNumber) {
        return UNSAFE.getLong(fieldNumberToAddress.get(fieldNumber));
    }

    @Override
    public long getTimestampValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return UNSAFE.getLong(repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber)));
    }

    @Override
    public void setTimestampValue(int fieldNumber, long newValue) {
        UNSAFE.putLong(fieldAddress(fieldNumber, MemoryConstants.LONG_SIZE), newValue);
        fieldsSet.set(fieldNumber);
    }

    @Override
    public void setTimestampValue(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, long newValue) {
        final boolean newRepeatingGroup = saveIndexIfGreater(groupNumber, repetitionIndex);
        UNSAFE.putLong(fieldAddress(fieldNumber, groupNumber, repetitionIndex, parentRepetitionIndex, MemoryConstants.LONG_SIZE), newValue);
        if (newRepeatingGroup) {
            fieldsSet.set(groupNumber);
        }
    }

    private long fieldAddress(int fieldNumber, short fieldSize) {
        long fieldAddress = fieldNumberToAddress.get(fieldNumber);
        if (fieldAddress == NO_VALUE) {
            fieldAddress = assignNewAddress(fieldSize);
            fieldNumberToAddress.put(fieldNumber, fieldAddress);
        }
        return fieldAddress;
    }

    private long fieldAddress(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, short fieldSize) {
        final long repeatingGroupKey = FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber);
        long fieldAddress = repeatingGroupAddresses.get(repeatingGroupKey);
        if (fieldAddress == NO_VALUE) {
            fieldAddress = assignNewAddress(fieldSize);
            repeatingGroupAddresses.put(repeatingGroupKey, fieldAddress);
        }
        return fieldAddress;
    }

    private long fieldAddressWithLengthCheck(int fieldNumber, short fieldSize) {
        if (fieldSize > regionSize) {
            throw new IllegalArgumentException(String.format(REGION_SIZE_ERROR_MSG_TEMPLATE, fieldSize, regionSize));
        }
        long fieldAddress = fieldNumberToAddress.get(fieldNumber);
        if (fieldAddress == NO_VALUE) {
            fieldAddress = assignNewAddress(fieldSize);
            fieldNumberToAddress.put(fieldNumber, fieldAddress);
        } else {
            final int currentLength = UNSAFE.getChar(fieldAddress);
            if (fieldSize > currentLength) {
                fieldAddress = assignNewAddress(fieldSize);
                fieldNumberToAddress.put(fieldNumber, fieldAddress);
            }
        }
        return fieldAddress;
    }

    private long fieldAddressWithLengthCheck(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex, short fieldSize) {
        if (fieldSize > regionSize) {
            throw new IllegalArgumentException(String.format(REGION_SIZE_ERROR_MSG_TEMPLATE, fieldSize, regionSize));
        }
        final long repeatingGroupKey = FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber);
        long fieldAddress = repeatingGroupAddresses.get(repeatingGroupKey);
        if (fieldAddress == NO_VALUE) {
            fieldAddress = assignNewAddress(fieldSize);
            repeatingGroupAddresses.put(repeatingGroupKey, fieldAddress);
        } else {
            final int currentLength = UNSAFE.getChar(fieldAddress);
            if (fieldSize > currentLength) {
                fieldAddress = assignNewAddress(fieldSize);
                repeatingGroupAddresses.put(repeatingGroupKey, fieldAddress);
            }
        }
        return fieldAddress;
    }

    private long assignNewAddress(short fieldSize) {
        long fieldAddress;
        final Region region = currentRegion();
        fieldAddress = region.append(fieldSize);
        if (fieldAddress == Region.NO_SPACE_AVAILABLE) {
            fieldAddress = nextRegion().append(fieldSize);
        }
        return fieldAddress;
    }

    private static void doSetCharSequenceData(CharSequence newValue, int numberOfChars, long fieldAddress) {
        UNSAFE.putChar(fieldAddress, (char) numberOfChars);
        final long dataStartingAddress = fieldAddress + FieldConstants.CHAR_SEQUENCE_LENGTH_SIZE;
        for (int i = 0; i < numberOfChars; i++) {
            UNSAFE.putChar(dataStartingAddress + (long) i * MemoryConstants.CHAR_SIZE, newValue.charAt(i));
        }
    }

    private static void doSetCharSequenceData(char[] newValue, int numberOfChars, long fieldAddress) {
        UNSAFE.putChar(fieldAddress, (char) numberOfChars);
        final long dataStartingAddress = fieldAddress + FieldConstants.CHAR_SEQUENCE_LENGTH_SIZE;
        for (int i = 0; i < numberOfChars; i++) {
            UNSAFE.putChar(dataStartingAddress + (long) i * MemoryConstants.CHAR_SIZE, ArrayUtils.getElementAt(newValue, i));
        }
    }

    private static void doSetCharSequenceData(ByteBuf newValue, int numberOfChars, long fieldAddress) {
        UNSAFE.putChar(fieldAddress, (char) numberOfChars);
        final ValueHolders.LongHolder address = new ValueHolders.LongHolder();
        address.setValue(fieldAddress + FieldConstants.CHAR_SEQUENCE_LENGTH_SIZE);
        newValue.forEachByte(byteRead -> {
            UNSAFE.putChar(address.getValue(), AsciiString.b2c(byteRead));
            address.increment(MemoryConstants.CHAR_SIZE);
            return true;
        });
    }

    private static void doSetDoubleValue(long newValue, short newScale, long fieldAddress) {
        UNSAFE.putLong(fieldAddress, newValue);
        UNSAFE.putShort(fieldAddress + MemoryConstants.LONG_SIZE, newScale);
    }

    @Nonnull
    private Region currentRegion() {
        Region region = ArrayUtils.getElementAt(regions, regionsIndex);
        if (region == null) {
            region = newRegion();
        }
        return region;
    }

    @Nonnull
    private Region newRegion() {
        final Region region = regionPool.tryGetAndRetain();
        if (region == null) {
            throw new FIXYouException(NO_REGIONS_IN_POOL_MSG_TEMPLATE + regionPool);
        } else {
            ensureRegionsLength(regionsIndex + 1);
            ArrayUtils.putElementAt(regions, regionsIndex, region);
        }
        return region;
    }

    @Nonnull
    private Region nextRegion() {
        regionsIndex++;
        final Region region = ArrayUtils.getElementAt(regions, regionsIndex);
        if (region == null) {
            return newRegion();
        } else {
            return region;
        }
    }

    private void ensureRegionsLength(int requiredLength) {
        if (regions.length < requiredLength) {
            final Region[] newRegions = new Region[requiredLength];
            System.arraycopy(regions, 0, newRegions, 0, regions.length);
            for (int i = regions.length; i < requiredLength; i++) {
                final Region newRegion = regionPool.tryGetAndRetain();
                if (newRegion == null) {
                    throw new FIXYouException(NO_REGIONS_IN_POOL_MSG_TEMPLATE + regionPool);
                }
                ArrayUtils.putElementAt(newRegions, i, newRegion);
            }
            ReflectionUtils.setFinalField(this, Fields.regions, newRegions);
        }
    }

    @Override
    protected void deallocate() {
        reset();
        super.deallocate();
    }

    @Override
    public void reset() {
        regionsIndex = 0;
        bodyLength = 0;
        fieldsSet.clear();
        fieldNumberToAddress.clear();
        repeatingGroupAddresses.clear();
        for (int i = 0; i < regions.length; i++) {
            final Region region = ArrayUtils.getElementAt(regions, i);
            if (region != null) {
                region.reset();
            }
        }
    }

    @Override
    public void close() {
        for (int i = 0; i < regions.length; i++) {
            final Region region = ArrayUtils.getElementAt(regions, i);
            if (region != null) {
                region.reset();
                regionPool.returnObject(region);
            }
        }
    }

    @Override
    public boolean isValueSet(int fieldNumber) {
        return fieldsSet.get(fieldNumber);
    }

    @Override
    public boolean isValueSet(int fieldNumber, int groupNumber, byte repetitionIndex, byte parentRepetitionIndex) {
        return repeatingGroupAddresses.containsKey(FixMessageRepeatingGroupUtils.repeatingGroupKey(parentRepetitionIndex, groupNumber, repetitionIndex, fieldNumber));
    }

    @Override
    public String toString() {
        return toString(false, StandardHeaderAndFooterOnlyFixSpec.INSTANCE);
    }

    @Override
    public String toString(boolean wholeMessage, FixSpec fixSpec) {
        return FixMessageToString.toString(this, wholeMessage, fixSpec);
    }

    @Override
    public void copyDataFrom(OffHeapFixMessage source) { //TODO maybe this method can be optimized?
        this.regionsIndex = source.regionsIndex;
        final int numberOfRegions = regionsIndex + 1;
        ensureRegionsLength(numberOfRegions);
        final long[] regionsStartAddresses = new long[numberOfRegions];
        copyRegionDataAndSaveStartAddresses(source, numberOfRegions, regionsStartAddresses);

        for (final IntLongCursor fieldNumberToAddressCursor : source.fieldNumberToAddress) {
            long regionStartingAddress = 0;
            int regionIndex = 0;
            for (; regionIndex < numberOfRegions; regionIndex++) {
                regionStartingAddress = ArrayUtils.getElementAt(regionsStartAddresses, regionIndex);
                if (regionStartingAddress <= fieldNumberToAddressCursor.value && (fieldNumberToAddressCursor.value - regionStartingAddress) < regionSize) {
                    break;
                }
            }
            fieldNumberToAddress.put(fieldNumberToAddressCursor.key, fieldNumberToAddressCursor.value - regionStartingAddress + ArrayUtils.getElementAt(regions, regionIndex).getStartingAddress());
        }

        for (final LongLongCursor fieldNumberToAddressCursor : source.repeatingGroupAddresses) {
            long regionStartingAddress = 0;
            int regionIndex = 0;
            for (; regionIndex < numberOfRegions; regionIndex++) {
                regionStartingAddress = ArrayUtils.getElementAt(regionsStartAddresses, regionIndex);
                if (regionStartingAddress <= fieldNumberToAddressCursor.value && (fieldNumberToAddressCursor.value - regionStartingAddress) < regionSize) {
                    break;
                }
            }
            repeatingGroupAddresses.put(fieldNumberToAddressCursor.key, fieldNumberToAddressCursor.value - regionStartingAddress + ArrayUtils.getElementAt(regions, regionIndex).getStartingAddress());
        }
    }

    private void copyRegionDataAndSaveStartAddresses(OffHeapFixMessage source, int numberOfRegions, long[] regionsStartAddresses) {
        for (int i = 0; i < numberOfRegions; i++) {
            final Region sourceRegion = ArrayUtils.getElementAt(source.regions, i);
            Region region = ArrayUtils.getElementAt(regions, i);
            if (region == null) {
                region = regionPool.tryGetAndRetain();
                if (region == null) {
                    throw new FIXYouException(NO_REGIONS_IN_POOL_MSG_TEMPLATE + regionPool);
                } else {
                    ArrayUtils.putElementAt(regions, i, region);
                }
            }
            region.copyDataFrom(sourceRegion);
            ArrayUtils.putElementAt(regionsStartAddresses, i, sourceRegion.getStartingAddress());
        }
    }

    private boolean saveIndexIfGreater(int groupNumber, byte index) {
        long groupNumberValueAddress = fieldNumberToAddress.get(groupNumber);
        final long newNumberOfRepetitions = (long) index + 1;
        if (groupNumberValueAddress != NO_VALUE) {
            final long currentNumberOfRepetitions = UNSAFE.getLong(groupNumberValueAddress);
            if (newNumberOfRepetitions > currentNumberOfRepetitions) {
                UNSAFE.putLong(groupNumberValueAddress, newNumberOfRepetitions);
            }
            return false;
        } else {
            groupNumberValueAddress = assignNewAddress(MemoryConstants.LONG_SIZE);
            fieldNumberToAddress.put(groupNumber, groupNumberValueAddress);
            UNSAFE.putLong(groupNumberValueAddress, newNumberOfRepetitions);
            return true;
        }
    }

    @Override
    public void removeField(int fieldNumber) {
        fieldsSet.clear(fieldNumber);
        fieldNumberToAddress.remove(fieldNumber);
    }

    @Override
    public IntCollection setFields() {
        return fieldNumberToAddress.keys();
    }

    @Override
    public int getBodyLength() {
        return bodyLength;
    }

    @Override
    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    private static final class DirectCharSequence implements CharSequence { //TODO write optimized equals

        private long startAddress;

        @Override
        public int length() {
            return UNSAFE.getChar(startAddress);
        }

        @Override
        public char charAt(int index) {
            return UNSAFE.getChar(startAddress + FieldConstants.CHAR_SEQUENCE_LENGTH_SIZE + ((long) MemoryConstants.CHAR_SIZE * index));
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException("DirectCharSequence does not support SubSequence method");
        }

        @Override
        public String toString() {
            if (startAddress > 0) {
                return new String(codePoints().toArray(), 0, length());
            } else {
                return "N/A";
            }
        }
    }
}
