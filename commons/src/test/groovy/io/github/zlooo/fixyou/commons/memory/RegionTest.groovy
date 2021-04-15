package io.github.zlooo.fixyou.commons.memory

import io.github.zlooo.fixyou.utils.UnsafeAccessor
import spock.lang.Specification

import java.util.concurrent.ThreadLocalRandom

class RegionTest extends Specification {

    private Region region = new Region(1L, 30 as short)

    def "should append to new region"() {
        when:
        def address = region.append(10 as short)

        then:
        address == 1
        region.@bytesTaken == 10
    }

    def "should append to region containing data"() {
        setup:
        region.append(10 as short)

        when:
        def address = region.append(10 as short)

        then:
        address == 11
        region.@bytesTaken == 20
    }

    def "should not append to region because of insufficient capacity"() {
        setup:
        region.append(15 as short)

        when:
        def address = region.append(20 as short)

        then:
        address == Region.NO_SPACE_AVAILABLE
        region.@bytesTaken == 15
    }

    def "should reset region"() {
        setup:
        region.append(10 as short)

        when:
        region.reset()

        then:
        region.@bytesTaken == 0
    }

    def "should reset before deallocation"(){
        setup:
        region.append(10 as short)

        when:
        region.deallocate()

        then:
        region.@bytesTaken == 0
    }

    def "should copy data from one region to other"() {
        setup:
        def region1 = new Region(UnsafeAccessor.UNSAFE.allocateMemory(30), 30 as short)
        def region2 = new Region(UnsafeAccessor.UNSAFE.allocateMemory(30), 30 as short)
        def data = new byte[30]
        ThreadLocalRandom.current().nextBytes(data)
        data.eachWithIndex { byte value, int index -> UnsafeAccessor.UNSAFE.putByte(region1.startingAddress + index, value) }

        when:
        region2.copyDataFrom(region1)

        then:
        def result = new byte[30]
        for (int i = 0; i < 30; i++) {
            result[i] = UnsafeAccessor.UNSAFE.getByte(region2.startingAddress + i)
        }
        result == data
    }
}
