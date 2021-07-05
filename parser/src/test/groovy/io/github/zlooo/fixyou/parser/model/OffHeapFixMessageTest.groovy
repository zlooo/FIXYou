package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.DefaultConfiguration
import io.github.zlooo.fixyou.commons.memory.RegionPool
import io.github.zlooo.fixyou.commons.utils.Comparators
import io.github.zlooo.fixyou.utils.UnsafeAccessor
import net.bytebuddy.utility.RandomString
import org.assertj.core.api.Assertions
import spock.lang.Shared
import spock.lang.Specification
import sun.misc.Unsafe

import java.time.Instant
import java.time.temporal.ChronoUnit

class OffHeapFixMessageTest extends Specification {

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
        fixMessage.fieldNumberToAddress.put(1, address)

        expect:
        fixMessage.getBooleanValue(1) == expectedValue

        where:
        value     | expectedValue
        1 as byte | true
        0 as byte | false
    }

    def "should get char value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        UNSAFE.putChar(address, value)
        fixMessage.fieldNumberToAddress.put(1, address)

        expect:
        fixMessage.getCharValue(1) == value

        where:
        value << ['a' as char, 'A' as char, '1' as char, '0' as char, 'b' as char, '/' as char]
    }

    def "should get char sequence value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        fixMessage.fieldNumberToAddress.put(1, address)
        UNSAFE.putChar(address, value.length() as char)
        address += 2
        value.chars.each {
            UNSAFE.putChar(address, it)
            address += 2
        }

        expect:
        Comparators.compare(fixMessage.getCharSequenceValue(1), value) == 0

        where:
        value << ['a', 'Aa', '1234abc', '/()*HHa']
    }

    def "should get double unscaled value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        fixMessage.fieldNumberToAddress.put(1, address)
        UNSAFE.putLong(address, unscaledValue)
        UNSAFE.putShort(address + 8, scale)

        expect:
        fixMessage.getDoubleUnscaledValue(1) == unscaledValue

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
        fixMessage.fieldNumberToAddress.put(1, address)
        UNSAFE.putLong(address, unscaledValue)
        UNSAFE.putShort(address + 8, scale)

        expect:
        fixMessage.getScale(1) == scale

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
        fixMessage.fieldNumberToAddress.put(1, address)
        UNSAFE.putLong(address, value)

        expect:
        fixMessage.getLongValue(1) == value

        where:
        value << [0L, 123L, 666777L, 666777888L]
    }

    def "should get timestamp value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        fixMessage.fieldNumberToAddress.put(1, address)
        UNSAFE.putLong(address, value)

        expect:
        fixMessage.getTimestampValue(1) == value

        where:
        value << [NOW.toEpochMilli(), NOW.plusSeconds(10).toEpochMilli(), NOW.plus(1, ChronoUnit.DAYS).toEpochMilli(), NOW.plus(60, ChronoUnit.DAYS).toEpochMilli(), NOW.plus(3 * 356, ChronoUnit.DAYS).toEpochMilli()]
    }

    def "should set boolean value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setBooleanValue(1, value)

        then:
        UNSAFE.getByte(address) == expectedValue
        fixMessage.fieldNumberToAddress.get(1) == address
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldsSet.get(1)
        fixMessage.repeatingGroupAddresses.isEmpty()

        where:
        value | expectedValue
        true  | 1 as byte
        false | 0 as byte
    }

    def "should set char value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setCharValue(1, value)

        then:
        UNSAFE.getChar(address) == value
        fixMessage.fieldNumberToAddress.get(1) == address
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldsSet.get(1)
        fixMessage.repeatingGroupAddresses.isEmpty()

        where:
        value << ['a' as char, 'A' as char, '1' as char, '0' as char, 'b' as char, '/' as char]
    }

    def "should set char sequence value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setCharSequenceValue(1, value)

        then:
        UNSAFE.getChar(address) == value.length()
        value.chars.eachWithIndex { char letter, int index ->
            assert UNSAFE.getChar(address + (index + 1) * 2) == letter
        }
        fixMessage.fieldNumberToAddress.get(1) == address
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldsSet.get(1)
        fixMessage.repeatingGroupAddresses.isEmpty()

        where:
        value << ['a', 'Aa', '1234abc', '/()*HHa']
    }

    def "should set char array value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setCharSequenceValue(1, value)

        then:
        UNSAFE.getChar(address) == value.length
        value.eachWithIndex { char letter, int index ->
            assert UNSAFE.getChar(address + (index + 1) * 2) == letter
        }
        fixMessage.fieldNumberToAddress.get(1) == address
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldsSet.get(1)
        fixMessage.repeatingGroupAddresses.isEmpty()

        where:
        value << ['a'.chars, 'Aa'.chars, '1234abc'.chars, '/()*HHa'.chars]
    }

    def "should set char array value with limited length"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setCharSequenceValue(1, value, length)

        then:
        UNSAFE.getChar(address) == length
        expectedValue.chars.eachWithIndex { char letter, int index ->
            assert UNSAFE.getChar(address + (index + 1) * 2) == letter
        }
        fixMessage.fieldNumberToAddress.get(1) == address
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldsSet.get(1)
        fixMessage.repeatingGroupAddresses.isEmpty()

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
        fixMessage.setDoubleValue(1, unscaledValue, scale)

        then:
        UNSAFE.getLong(address) == unscaledValue
        UNSAFE.getShort(address + 8) == scale
        fixMessage.fieldNumberToAddress.get(1) == address
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldsSet.get(1)
        fixMessage.repeatingGroupAddresses.isEmpty()

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
        fixMessage.setLongValue(1, value)

        then:
        UNSAFE.getLong(address) == value
        fixMessage.fieldNumberToAddress.get(1) == address
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldsSet.get(1)
        fixMessage.repeatingGroupAddresses.isEmpty()

        where:
        value << [0L, 123L, 666777L, 666777888L]
    }

    def "should set timestamp value"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()

        when:
        fixMessage.setTimestampValue(1, value)

        then:
        UNSAFE.getLong(address) == value
        fixMessage.fieldNumberToAddress.get(1) == address
        fixMessage.fieldNumberToAddress.size() == 1
        fixMessage.fieldsSet.get(1)
        fixMessage.repeatingGroupAddresses.isEmpty()

        where:
        value << [NOW.toEpochMilli(), NOW.plusSeconds(10).toEpochMilli(), NOW.plus(1, ChronoUnit.DAYS).toEpochMilli(), NOW.plus(60, ChronoUnit.DAYS).toEpochMilli(), NOW.plus(3 * 356, ChronoUnit.DAYS).toEpochMilli()]
    }

    def "should put 2 values in same region if they fit"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        fixMessage.setBooleanValue(1, true)

        when:
        fixMessage.setBooleanValue(2, true)

        then:
        UNSAFE.getByte(address) == 1 as byte
        UNSAFE.getByte(address + 1) == 1 as byte
        fixMessage.fieldNumberToAddress.get(1) == address
        fixMessage.fieldNumberToAddress.get(2) == address + 1
        fixMessage.fieldNumberToAddress.size() == 2
        fixMessage.fieldsSet.get(1)
        fixMessage.fieldsSet.get(2)
        fixMessage.repeatingGroupAddresses.isEmpty()
        fixMessage.regionsIndex == 0
        Assertions.assertThat(fixMessage.regions[0]).isNotNull()
        Assertions.assertThat(fixMessage.regions.drop(1)).containsOnlyNulls()
    }

    def "should allocate new region if new value does not fit"() {
        setup:
        def address = fixMessage.currentRegion().getStartingAddress()
        fixMessage.setCharSequenceValue(1, RandomString.make(125))

        when:
        fixMessage.setLongValue(2, 666)

        then:
        UNSAFE.getChar(address) == 125 as char
        fixMessage.fieldNumberToAddress.get(1) == address
        fixMessage.fieldNumberToAddress.size() == 2
        fixMessage.fieldsSet.get(1)
        fixMessage.fieldsSet.get(2)
        fixMessage.repeatingGroupAddresses.isEmpty()
        fixMessage.regionsIndex == 1
        Assertions.assertThat(fixMessage.regions[0]).isNotNull()
        Assertions.assertThat(fixMessage.regions[1]).isNotNull()
        Assertions.assertThat(fixMessage.regions.drop(2)).containsOnlyNulls()
        def secondRegionStartAddress = fixMessage.currentRegion().getStartingAddress()
        address != secondRegionStartAddress
        fixMessage.fieldNumberToAddress.get(2) == secondRegionStartAddress
        UNSAFE.getLong(secondRegionStartAddress) == 666
    }

    def "should not allow to set field value that is larger than region"() {
        when:
        fixMessage.setCharSequenceValue(1, RandomString.make(DefaultConfiguration.REGION_SIZE))

        then:
        thrown(IllegalArgumentException)
        fixMessage.fieldNumberToAddress.isEmpty()
        !fixMessage.fieldsSet.get(1)
    }

    def "should ensure that regions array has sufficient capacity"() {
        setup:
        def numberOfRegions = fixMessage.regions.length
        def regions = fixMessage.regions.clone()

        when:
        fixMessage.ensureRegionsLength(numberOfRegions + 1)

        then:
        Assertions.assertThat(fixMessage.regions.dropRight(1)).containsExactlyElementsOf(regions.toList())
        Assertions.assertThat(fixMessage.regions.drop(numberOfRegions)).hasSize(1).doesNotContainNull()
    }

    def "should reset message"() {
        setup:
        fixMessage.setCharSequenceValue(1, RandomString.make(125))
        fixMessage.setLongValue(3, 2, 0 as byte, 0 as byte, 666)

        when:
        fixMessage.reset()

        then:
        fixMessage.fieldNumberToAddress.isEmpty()
        fixMessage.repeatingGroupAddresses.isEmpty()
        fixMessage.fieldsSet.isEmpty()
        fixMessage.regionsIndex == 0
        Assertions.assertThat(fixMessage.regions).allMatch({ it == null || it.bytesTaken == 0 })
    }

    def "should close fix message"() {
        setup:
        fixMessage.setCharSequenceValue(1, RandomString.make(125))
        fixMessage.setLongValue(3, 2, 0 as byte, 0 as byte, 666)

        when:
        fixMessage.close()

        then:
        Assertions.assertThat(fixMessage.regions).allMatch({ it == null || it.bytesTaken == 0 })
        fixMessage.regionPool.areAllObjectsReturned()
    }

    def "should copy data"() {
        setup:
        def field1Value = RandomString.make(125)
        fixMessage.setCharSequenceValue(1, field1Value)
        fixMessage.setLongValue(3, 2, 0 as byte, 0 as byte, 666)
        def dstFixMessage = new OffHeapFixMessage(regionPool)

        when:
        dstFixMessage.copyDataFrom(fixMessage)

        then:
        Comparators.compare(dstFixMessage.getCharSequenceValue(1), field1Value) == 0
        dstFixMessage.getLongValue(3, 2, 0 as byte, 0 as byte) == 666
        dstFixMessage.getLongValue(2) == 1

        cleanup:
        dstFixMessage?.close()
    }
}
