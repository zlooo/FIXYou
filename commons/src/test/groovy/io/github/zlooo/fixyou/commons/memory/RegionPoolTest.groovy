package io.github.zlooo.fixyou.commons.memory

import org.assertj.core.api.Assertions
import spock.lang.Specification

class RegionPoolTest extends Specification {

    private RegionPool regionPool

    void cleanup() {
        regionPool?.close()
    }

    def "should init pool"() {
        when:
        regionPool = new RegionPool(4, 128 as short)

        then:
        regionPool.@poolAddress > 0
        regionPool.@delegate!=null
        regionPool.@delegate.@objectArray.length == 4
        regionPool.@delegate.@objectArray[0].@startingAddress == regionPool.@poolAddress
        regionPool.@delegate.@objectArray[1].@startingAddress == regionPool.@poolAddress+128
        regionPool.@delegate.@objectArray[2].@startingAddress == regionPool.@poolAddress+256
        regionPool.@delegate.@objectArray[3].@startingAddress == regionPool.@poolAddress+384
        Assertions.assertThat(regionPool.@delegate.@objectArray as Region[]).extracting("size").containsOnly(128 as short)
    }
}
