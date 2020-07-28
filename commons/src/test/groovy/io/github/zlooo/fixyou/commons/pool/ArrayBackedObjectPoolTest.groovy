package io.github.zlooo.fixyou.commons.pool

import org.assertj.core.api.Assertions
import spock.lang.Specification

class ArrayBackedObjectPoolTest extends Specification {

    private ArrayBackedObjectPool objectPool = new ArrayBackedObjectPool(10, { -> new TestPoolableObject() }, TestPoolableObject, 100)

    def "should get object from pool"() {
        when:
        def result = objectPool.getAndRetain()

        then:
        result.refCnt() == 1
        result.getState().get() == AbstractPoolableObject.IN_USE_STATE
        objectPool.@objGetPosition == 1
        objectPool.@objectPutPosition == 16
        objectPool.@objectArray.findAll { it == null }.size() == 1
    }

    def "should get multiple objects from pool"() {
        when:
        def result1 = objectPool.getAndRetain()
        def result2 = objectPool.getAndRetain()
        def result3 = objectPool.getAndRetain()
        def result4 = objectPool.getAndRetain()

        then:
        result1.refCnt() == 1
        result1.getState().get() == AbstractPoolableObject.IN_USE_STATE
        result2.refCnt() == 1
        result2.getState().get() == AbstractPoolableObject.IN_USE_STATE
        result3.refCnt() == 1
        result3.getState().get() == AbstractPoolableObject.IN_USE_STATE
        result4.refCnt() == 1
        result4.getState().get() == AbstractPoolableObject.IN_USE_STATE
        objectPool.@objGetPosition == 4
        objectPool.@objectPutPosition == 16
        objectPool.@objectArray.findAll { it == null }.size() == 4
    }

    def "should return object to pool"() {
        setup:
        def object = objectPool.getAndRetain()

        when:
        object.release()

        then:
        object.refCnt() == 0
        object.getState().get() == AbstractPoolableObject.AVAILABLE_STATE
        objectPool.@objGetPosition == 1
        objectPool.@objectPutPosition == 17
        objectPool.@objectArray.findAll { it == null }.isEmpty()
    }

    def "should return multiple objects to pool"() {
        setup:
        def object1 = objectPool.getAndRetain()
        def object2 = objectPool.getAndRetain()
        def object3 = objectPool.getAndRetain()
        def object4 = objectPool.getAndRetain()

        when:
        object1.release()
        object2.release()
        object3.release()
        object4.release()

        then:
        object1.refCnt() == 0
        object1.getState().get() == AbstractPoolableObject.AVAILABLE_STATE
        object2.refCnt() == 0
        object2.getState().get() == AbstractPoolableObject.AVAILABLE_STATE
        object3.refCnt() == 0
        object3.getState().get() == AbstractPoolableObject.AVAILABLE_STATE
        object4.refCnt() == 0
        object4.getState().get() == AbstractPoolableObject.AVAILABLE_STATE
        objectPool.@objGetPosition == 4
        objectPool.@objectPutPosition == 20
        objectPool.@objectArray.findAll { it == null }.isEmpty()
    }

    def "should not throw illegal state exception when returning object is in available state"() {
        setup:
        def poolableObject = objectPool.getAndRetain()
        poolableObject.getState().set(AbstractPoolableObject.AVAILABLE_STATE)

        when:
        objectPool.returnObject(poolableObject)

        then:
        poolableObject.getState().get() == AbstractPoolableObject.AVAILABLE_STATE
    }

    def "should throw illegal state exception when returning object is in neither available nor in use state"() {
        setup:
        def poolableObject = objectPool.getAndRetain()
        poolableObject.getState().set(666)

        when:
        objectPool.returnObject(poolableObject)

        then:
        thrown(IllegalStateException)
    }

    def "should close all objects in pool when pool is closed"() {
        setup:
        def objectsInPool = objectPool.@objectArray.collect()

        when:
        objectPool.close()

        then:
        Assertions.assertThat(objectPool.@objectArray).containsOnlyNulls()
        Assertions.assertThat(objectsInPool).allMatch({ it -> it.closeInvoked })
    }

    def "should resize pool when it turn out to be too small"() {
        setup:
        def originalPoolSize = objectPool.@objectArray.length
        def objectsFromPool = []

        when:
        for (i in 0..originalPoolSize) {
            objectsFromPool << objectPool.getAndRetain()
        }

        then:
        Assertions.assertThat(objectsFromPool).doesNotContainNull().hasSize(originalPoolSize + 1)
        objectPool.@objectArray.length > originalPoolSize
    }

    def "should try get and receive null when all elements from pool are taken"() {
        setup:
        def originalPoolSize = objectPool.@objectArray.length
        def objectsFromPool = []
        for (i in 1..originalPoolSize) {
            objectsFromPool << objectPool.getAndRetain()
        }

        when:
        def result = objectPool.tryGetAndRetain()

        then:
        result == null
        objectPool.@objectArray.length == originalPoolSize
        Assertions.assertThat(objectPool.@objectArray).containsOnlyNulls()
    }

    def "should try get and receive object when all elements from pool are taken and then one is returned"() {
        setup:
        def originalPoolSize = objectPool.@objectArray.length
        def objectsFromPool = []
        for (i in 1..originalPoolSize) {
            objectsFromPool << objectPool.getAndRetain()
        }
        objectPool.returnObject(objectsFromPool[0])

        when:
        def result = objectPool.tryGetAndRetain()

        then:
        result == objectsFromPool[0]
        objectPool.@objectArray.length == originalPoolSize
        Assertions.assertThat(objectPool.@objectArray).containsOnlyNulls()
        objectPool.@objectPutPosition == 17
        objectPool.@objGetPosition == 17
    }

    static class TestPoolableObject extends AbstractPoolableObject {

        boolean closeInvoked

        @Override
        void close() {
            closeInvoked = true
        }
    }
}
