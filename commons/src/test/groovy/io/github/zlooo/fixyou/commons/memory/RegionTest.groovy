package io.github.zlooo.fixyou.commons.memory

import spock.lang.Specification

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
}
