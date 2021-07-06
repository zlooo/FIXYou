package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.DefaultConfiguration
import io.github.zlooo.fixyou.commons.memory.RegionPool
import io.github.zlooo.fixyou.commons.utils.Comparators
import io.github.zlooo.fixyou.utils.UnsafeAccessor
import spock.lang.Shared
import spock.lang.Specification
import sun.misc.Unsafe

import java.time.Instant
import java.time.temporal.ChronoUnit

class FixMessageRepeatingGroupTest extends Specification {

    private static final Unsafe UNSAFE = UnsafeAccessor.UNSAFE
    private static final Instant NOW = Instant.now()

    @Shared
    private RegionPool regionPool = new RegionPool(10, DefaultConfiguration.REGION_SIZE)
    @Shared
    private OffHeapFixMessage fixMessage = new OffHeapFixMessage(regionPool)

    void setup() {
        fixMessage.currentRegion() //just to make sure region is allocated if needed which is during execution of first test from this class
    }

    void cleanup() {
        fixMessage.reset()
    }

    def cleanupSpec() {
        fixMessage?.close()
        regionPool?.close()
    }

    def "should get boolean value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        UNSAFE.putByte(address, value)
        fixMessage.repeatingGroupAddresses.put(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1), address)

        expect:
        fixMessage.getBooleanValue(1, 3, 2 as byte, 4 as byte) == expectedValue

        where:
        value     | expectedValue
        1 as byte | true
        0 as byte | false
    }

    def "should get char value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        UNSAFE.putChar(address, value)
        fixMessage.repeatingGroupAddresses.put(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1), address)

        expect:
        fixMessage.getCharValue(1, 3, 2 as byte, 4 as byte) == value

        where:
        value << ['a' as char, 'A' as char, '1' as char, '0' as char, 'b' as char, '/' as char]
    }

    def "should get char sequence value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        fixMessage.repeatingGroupAddresses.put(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1), address)
        UNSAFE.putChar(address, value.length() as char)
        address += 2
        value.chars.each {
            UNSAFE.putChar(address, it)
            address += 2
        }

        expect:
        Comparators.compare(fixMessage.getCharSequenceValue(1, 3, 2 as byte, 4 as byte), value) == 0

        where:
        value << ['a', 'Aa', '1234abc', '/()*HHa']
    }

    def "should get double unscaled value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        fixMessage.repeatingGroupAddresses.put(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1), address)
        UNSAFE.putLong(address, unscaledValue)
        UNSAFE.putShort(address + 8, scale)

        expect:
        fixMessage.getDoubleUnscaledValue(1, 3, 2 as byte, 4 as byte) == unscaledValue

        where:
        unscaledValue | scale
        0             | 0 as short
        123           | 0 as short
        666777        | 3 as short
        666777888     | 1 as short
    }

    def "should get scale"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        fixMessage.repeatingGroupAddresses.put(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1), address)
        UNSAFE.putLong(address, unscaledValue)
        UNSAFE.putShort(address + 8, scale)

        expect:
        fixMessage.getScale(1, 3, 2 as byte, 4 as byte) == scale

        where:
        unscaledValue | scale
        0             | 0 as short
        123           | 0 as short
        666777        | 3 as short
        666777888     | 1 as short
    }

    def "should get long value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        fixMessage.repeatingGroupAddresses.put(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1), address)
        UNSAFE.putLong(address, value)

        expect:
        fixMessage.getLongValue(1, 3, 2 as byte, 4 as byte) == value

        where:
        value << [0L, 123L, 666777L, 666777888L]
    }

    def "should get timestamp value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        fixMessage.repeatingGroupAddresses.put(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1), address)
        UNSAFE.putLong(address, value)

        expect:
        fixMessage.getTimestampValue(1, 3, 2 as byte, 4 as byte) == value

        where:
        value << [NOW.toEpochMilli(), NOW.plusSeconds(10).toEpochMilli(), NOW.plus(1, ChronoUnit.DAYS).toEpochMilli(), NOW.plus(60, ChronoUnit.DAYS).toEpochMilli(), NOW.plus(3 * 356, ChronoUnit.DAYS).toEpochMilli()]
    }

    def "should set boolean value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setBooleanValue(1, 3, 2 as byte, 4 as byte, value)

        then:
        UNSAFE.getByte(address + 8) == expectedValue //+8 because first 8 bytes are storing number of repetitions for group 3
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldNumberToAddress.containsKey(3)
        UNSAFE.getLong(fixMessage.fieldNumberToAddress.get(3)) == 2 + 1 //+1 because this field holds number of repetitions NOT repetition index
        fixMessage.fieldsSet.get(3)
        fixMessage.repeatingGroupAddresses.size() == 1
        fixMessage.repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1)) == address + 8 //+8 because first 8 bytes are storing number of repetitions for group 3

        where:
        value | expectedValue
        true  | 1 as byte
        false | 0 as byte
    }

    def "should set char value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setCharValue(1, 3, 2 as byte, 4 as byte, value)

        then:
        UNSAFE.getChar(address + 8) == value //+8 because first 8 bytes are storing number of repetitions for group 3
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldNumberToAddress.containsKey(3)
        UNSAFE.getLong(fixMessage.fieldNumberToAddress.get(3)) == 2 + 1 //+1 because this field holds number of repetitions NOT repetition index
        fixMessage.fieldsSet.get(3)
        fixMessage.repeatingGroupAddresses.size() == 1
        fixMessage.repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1)) == address + 8 //+8 because first 8 bytes are storing number of repetitions for group 3

        where:
        value << ['a' as char, 'A' as char, '1' as char, '0' as char, 'b' as char, '/' as char]
    }

    def "should set char sequence value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setCharSequenceValue(1, 3, 2 as byte, 4 as byte, value)

        then:
        UNSAFE.getChar(address + 8) == value.length() //+8 because first 8 bytes are storing number of repetitions for group 3
        value.chars.eachWithIndex { char letter, int index ->
            assert UNSAFE.getChar(address + 8 + (index + 1) * 2) == letter
        }
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldNumberToAddress.containsKey(3)
        UNSAFE.getLong(fixMessage.fieldNumberToAddress.get(3)) == 2 + 1 //+1 because this field holds number of repetitions NOT repetition index
        fixMessage.fieldsSet.get(3)
        fixMessage.repeatingGroupAddresses.size() == 1
        fixMessage.repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1)) == address + 8 //+8 because first 8 bytes are storing number of repetitions for group 3

        where:
        value << ['a', 'Aa', '1234abc', '/()*HHa']
    }

    def "should set char array value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setCharSequenceValue(1, 3, 2 as byte, 4 as byte, value)

        then:
        UNSAFE.getChar(address + 8) == value.length //+8 because first 8 bytes are storing number of repetitions for group 3
        value.eachWithIndex { char letter, int index ->
            assert UNSAFE.getChar(address + 8 + (index + 1) * 2) == letter
        }
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldNumberToAddress.containsKey(3)
        UNSAFE.getLong(fixMessage.fieldNumberToAddress.get(3)) == 2 + 1 //+1 because this field holds number of repetitions NOT repetition index
        fixMessage.fieldsSet.get(3)
        fixMessage.repeatingGroupAddresses.size() == 1
        fixMessage.repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1)) == address + 8 //+8 because first 8 bytes are storing number of repetitions for group 3

        where:
        value << ['a'.chars, 'Aa'.chars, '1234abc'.chars, '/()*HHa'.chars]
    }

    def "should set char array value with limited length"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setCharSequenceValue(1, 3, 2 as byte, 4 as byte, value, length)

        then:
        UNSAFE.getChar(address + 8) == length //+8 because first 8 bytes are storing number of repetitions for group 3
        expectedValue.chars.eachWithIndex { char letter, int index ->
            assert UNSAFE.getChar(address + 8 + (index + 1) * 2) == letter
        }
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldNumberToAddress.containsKey(3)
        UNSAFE.getLong(fixMessage.fieldNumberToAddress.get(3)) == 2 + 1 //+1 because this field holds number of repetitions NOT repetition index
        fixMessage.fieldsSet.get(3)
        fixMessage.repeatingGroupAddresses.size() == 1
        fixMessage.repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1)) == address + 8 //+8 because first 8 bytes are storing number of repetitions for group 3

        where:
        value           | length | expectedValue
        'a'.chars       | 1      | 'a'
        'Aa'.chars      | 2      | 'Aa'
        'Aa'.chars      | 1      | 'A'
        '1234abc'.chars | 4      | '1234'
        '1234abc'.chars | 5      | '1234a'
        '/()*HHa'.chars | 7      | '/()*HHa'
    }

    def "should set double value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setDoubleValue(1, 3, 2 as byte, 4 as byte, unscaledValue, scale)

        then:
        UNSAFE.getLong(address + 8) == unscaledValue //+8 because first 8 bytes are storing number of repetitions for group 3
        UNSAFE.getShort(address + 8 + 8) == scale
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldNumberToAddress.containsKey(3)
        UNSAFE.getLong(fixMessage.fieldNumberToAddress.get(3)) == 2 + 1 //+1 because this field holds number of repetitions NOT repetition index
        fixMessage.fieldsSet.get(3)
        fixMessage.repeatingGroupAddresses.size() == 1
        fixMessage.repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1)) == address + 8 //+8 because first 8 bytes are storing number of repetitions for group 3

        where:
        unscaledValue | scale
        0             | 0 as short
        123           | 0 as short
        666777        | 3 as short
        666777888     | 1 as short
    }

    def "should set long value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setLongValue(1, 3, 2 as byte, 4 as byte, value)

        then:
        UNSAFE.getLong(address + 8) == value //+8 because first 8 bytes are storing number of repetitions for group 3
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldNumberToAddress.containsKey(3)
        UNSAFE.getLong(fixMessage.fieldNumberToAddress.get(3)) == 2 + 1 //+1 because this field holds number of repetitions NOT repetition index
        fixMessage.fieldsSet.get(3)
        fixMessage.repeatingGroupAddresses.size() == 1
        fixMessage.repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1)) == address + 8 //+8 because first 8 bytes are storing number of repetitions for group 3

        where:
        value << [0L, 123L, 666777L, 666777888L]
    }

    def "should set timestamp value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setTimestampValue(1, 3, 2 as byte, 4 as byte, value)

        then:
        UNSAFE.getLong(address + 8) == value //+8 because first 8 bytes are storing number of repetitions for group 3
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldNumberToAddress.containsKey(3)
        UNSAFE.getLong(fixMessage.fieldNumberToAddress.get(3)) == 2 + 1 //+1 because this field holds number of repetitions NOT repetition index
        fixMessage.fieldsSet.get(3)
        fixMessage.repeatingGroupAddresses.size() == 1
        fixMessage.repeatingGroupAddresses.get(FixMessageRepeatingGroupUtils.repeatingGroupKey(4 as byte, 3, 2 as byte, 1)) == address + 8 //+8 because first 8 bytes are storing number of repetitions for group 3

        where:
        value << [NOW.toEpochMilli(), NOW.plusSeconds(10).toEpochMilli(), NOW.plus(1, ChronoUnit.DAYS).toEpochMilli(), NOW.plus(60, ChronoUnit.DAYS).toEpochMilli(), NOW.plus(3 * 356, ChronoUnit.DAYS).toEpochMilli()]
    }

    def "should save greater repetition index"() {
        setup:
        fixMessage.setLongValue(1, 3, 1 as byte, 4 as byte, 1)

        when:
        fixMessage.setLongValue(1, 3, 2 as byte, 4 as byte, 1)

        then:
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldNumberToAddress.containsKey(3)
        UNSAFE.getLong(fixMessage.fieldNumberToAddress.get(3)) == 2 + 1
    }

    def "should not save lesser repetition index"() {
        setup:
        fixMessage.setLongValue(3, 3)

        when:
        fixMessage.setLongValue(1, 3, 2 as byte, 4 as byte, 1)

        then:
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldNumberToAddress.containsKey(3)
        UNSAFE.getLong(fixMessage.fieldNumberToAddress.get(3)) == 3
    }
}
