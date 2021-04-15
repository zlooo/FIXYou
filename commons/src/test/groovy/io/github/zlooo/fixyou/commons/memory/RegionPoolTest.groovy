package io.github.zlooo.fixyou.commons.memory

import io.github.zlooo.fixyou.commons.pool.ObjectPool
import io.github.zlooo.fixyou.commons.utils.ReflectionUtils
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
        regionPool.@delegate != null
        regionPool.@delegate.@objectArray.length == 4
        regionPool.@delegate.@objectArray[0].@startingAddress == regionPool.@poolAddress
        regionPool.@delegate.@objectArray[1].@startingAddress == regionPool.@poolAddress + 128
        regionPool.@delegate.@objectArray[2].@startingAddress == regionPool.@poolAddress + 256
        regionPool.@delegate.@objectArray[3].@startingAddress == regionPool.@poolAddress + 384
        Assertions.assertThat(regionPool.@delegate.@objectArray as Region[]).extracting("size").containsOnly(128 as short)
    }

    def "should try to get and retain object from underlying pool"() {
        setup:
        regionPool = new RegionPool(4, 128 as short)
        def underlyingPool = Mock(ObjectPool)
        regionPool.@delegate.close()
        ReflectionUtils.setFinalField(regionPool, "delegate", underlyingPool)
        def region = new Region(1, 1 as short)

        when:
        def result = regionPool.tryGetAndRetain()

        then:
        1 * underlyingPool.tryGetAndRetain() >> region
        result === region
        0 * _
    }

    def "should not get and retain object"() {
        setup:
        regionPool = new RegionPool(4, 128 as short)

        when:
        regionPool.getAndRetain()

        then:
        thrown(UnsupportedOperationException)
    }

    def "should return object to underlying pool"() {
        setup:
        regionPool = new RegionPool(4, 128 as short)
        def underlyingPool = Mock(ObjectPool)
        regionPool.@delegate.close()
        ReflectionUtils.setFinalField(regionPool, "delegate", underlyingPool)
        def region = new Region(1, 1 as short)

        when:
        regionPool.returnObject(region)

        then:
        1 * underlyingPool.returnObject(region)
        0 * _
    }

    def "should check if all objects are returned to underlying pool"() {
        setup:
        regionPool = new RegionPool(4, 128 as short)
        def underlyingPool = Mock(ObjectPool)
        regionPool.@delegate.close()
        ReflectionUtils.setFinalField(regionPool, "delegate", underlyingPool)

        when:
        def result = regionPool.areAllObjectsReturned()

        then:
        1 * underlyingPool.areAllObjectsReturned() >> true
        result
        0 * _
    }
}
