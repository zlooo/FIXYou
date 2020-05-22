package io.github.zlooo.fixyou.commons.pool

import org.assertj.core.api.Assertions
import org.assertj.core.api.Condition
import spock.lang.Specification

import java.lang.reflect.Modifier
import java.util.function.Consumer

class DefaultObjectPoolTest extends Specification {

    DefaultObjectPool<TestPoolableObject> objectPool = new DefaultObjectPool(10, { -> new TestPoolableObject()
    }, TestPoolableObject.class)

    def "should get and retain object from pool"() {
        when:
        def objectFromPool = objectPool.getAndRetain()

        then:
        objectFromPool.state.get() == AbstractPoolableObject.IN_USE_STATE
        objectFromPool.pool == objectPool
        objectFromPool.refCnt() == 1
        objectFromPool.is(objectPool.@firstObject)
        objectPool.@objGetPosition == 0
        objectPool.@objectPutPosition == objectPool.@objectArray.length
    }

    def "should get and retain object from pool using local var1"() {
        setup:
        objectPool.getAndRetain().release()

        when:
        def objectFromPool = objectPool.getAndRetain()

        then:
        objectFromPool.state.get() == AbstractPoolableObject.IN_USE_STATE
        objectFromPool.pool == objectPool
        objectFromPool.refCnt() == 1
        objectFromPool.is(objectPool.@firstObject)
        objectPool.@objGetPosition == 0
        objectPool.@objectPutPosition == objectPool.@objectArray.length
    }

    def "should get and retain object from pool using local var2"() {
        setup:
        objectPool.@firstObject.getState().set(AbstractPoolableObject.IN_USE_STATE)

        when:
        def objectFromPool = objectPool.getAndRetain()

        then:
        objectFromPool.state.get() == AbstractPoolableObject.IN_USE_STATE
        objectFromPool.pool == objectPool
        objectFromPool.refCnt() == 1
        objectFromPool.is(objectPool.@secondObject)
        objectPool.@objGetPosition == 0
        objectPool.@objectPutPosition == objectPool.@objectArray.length
    }

    def "should get and retain object from pool using object array"() {
        setup:
        objectPool.@firstObject.getState().set(AbstractPoolableObject.IN_USE_STATE)
        objectPool.@secondObject.getState().set(AbstractPoolableObject.IN_USE_STATE)

        when:
        def objectFromPool = objectPool.getAndRetain()

        then:
        objectFromPool.state.get() == AbstractPoolableObject.IN_USE_STATE
        objectFromPool.pool == objectPool
        objectFromPool.refCnt() == 1
        objectPool.@objGetPosition == 1
        objectPool.@objectPutPosition == objectPool.@objectArray.length
        objectPool.@objectArray[objectPool.@objGetPosition - 1] == null
    }

    def "should deallocate local variable object"() {
        setup:
        def objectFromPool = objectPool.getAndRetain()

        when:
        def releaseResult = objectFromPool.release()

        then:
        releaseResult
        objectFromPool.refCnt() == 0
        objectPool.@firstObject.getState().get() == AbstractPoolableObject.AVAILABLE_STATE
        objectPool.@secondObject.getState().get() == AbstractPoolableObject.AVAILABLE_STATE
        Assertions.assertThat(objectPool.@objectArray).doesNotContainNull().allMatch({ item -> item.getState().get() == AbstractPoolableObject.AVAILABLE_STATE })
        objectPool.@objGetPosition == 0
        objectPool.@objectPutPosition == objectPool.@objectArray.length
    }

    def "should deallocate array object"() {
        setup:
        objectPool.@firstObject.getState().set(AbstractPoolableObject.IN_USE_STATE)
        objectPool.@secondObject.getState().set(AbstractPoolableObject.IN_USE_STATE)
        def objectFromPool = objectPool.getAndRetain()

        when:
        def releaseResult = objectFromPool.release()

        then:
        releaseResult
        objectFromPool.refCnt() == 0
        objectPool.@firstObject.getState().get() == AbstractPoolableObject.IN_USE_STATE
        objectPool.@secondObject.getState().get() == AbstractPoolableObject.IN_USE_STATE
        Assertions.assertThat(objectPool.@objectArray).doesNotContainNull().allMatch({ item -> item.getState().get() == AbstractPoolableObject.AVAILABLE_STATE })
        objectPool.@objGetPosition == 1
        objectPool.@objectPutPosition == objectPool.@objectArray.length + 1
    }

    def "should manage get and put pointers appropriately"() {
        setup:
        objectPool.@firstObject.getState().set(AbstractPoolableObject.IN_USE_STATE)
        objectPool.@secondObject.getState().set(AbstractPoolableObject.IN_USE_STATE)
        def objectsTaken = []
        def releaseResults = []

        when:
        for (int i = 0; i < elementsTaken; i++) {
            objectsTaken << objectPool.getAndRetain()
        }
        for (int i = 0; i < elementsReleased; i++) {
            releaseResults << objectsTaken[i].release()
        }

        then:
        Assertions.assertThat(objectsTaken.subList(0, elementsReleased)).allMatch({ item -> item.getState().get() == AbstractPoolableObject.AVAILABLE_STATE }).allMatch({ item -> item.refCnt() == 0 })
        if (elementsTaken > elementsReleased) {
            Assertions.assertThat(objectsTaken.subList(elementsReleased, elementsTaken)).allMatch({ item -> item.getState().get() == AbstractPoolableObject.IN_USE_STATE }).allMatch({ item -> item.refCnt() == 1 })
        }
        Assertions.assertThat(objectsTaken.subList(elementsTaken, objectsTaken.size())).allMatch({ item -> item.getState().get() == AbstractPoolableObject.AVAILABLE_STATE }).allMatch({ item -> item.refCnt() == 0 })
        Assertions.assertThat(releaseResults.subList(0, elementsReleased)).allMatch({ item -> item })
        objectPool.@firstObject.getState().get() == AbstractPoolableObject.IN_USE_STATE
        objectPool.@secondObject.getState().get() == AbstractPoolableObject.IN_USE_STATE
        Assertions.
                assertThat(objectPool.@objectArray).
                haveExactly(elementsTaken - elementsReleased, new Condition<? extends AbstractPoolableObject>({ item -> item == null }, "Is null")).
                haveExactly(objectPool.@objectArray.length - elementsTaken + elementsReleased,
                            new Condition<? extends AbstractPoolableObject>({ item -> item?.getState()?.get() == AbstractPoolableObject.AVAILABLE_STATE }, "Is in available state"))
        objectPool.@objGetPosition == getPointer
        objectPool.@objectPutPosition == putPointer

        where:
        elementsTaken | elementsReleased | getPointer | putPointer
        1             | 1                | 1          | 16 + 1
        2             | 2                | 2          | 16 + 2
        5             | 1                | 5          | 16 + 1
    }

    def "should manage get and put pointers appropriately after then spin over"() {
        setup:
        objectPool.@firstObject.getState().set(AbstractPoolableObject.IN_USE_STATE)
        objectPool.@secondObject.getState().set(AbstractPoolableObject.IN_USE_STATE)
        def objectsTaken = []
        objectPool.@objectArray.length.times {
            objectsTaken << objectPool.getAndRetain()
        }
        objectsTaken.each { it.release() }
        objectsTaken.clear()
        def releaseResults = []

        when:
        1.upto(5) {
            objectsTaken << objectPool.getAndRetain()
        }
        0.upto(2) {
            releaseResults << objectsTaken[it].release()
        }

        then:
        Assertions.assertThat(objectsTaken.subList(0, 3)).allMatch({ item -> item.getState().get() == AbstractPoolableObject.AVAILABLE_STATE }).allMatch({ item -> item.refCnt() == 0 })
        Assertions.assertThat(objectsTaken.subList(3, 5)).allMatch({ item -> item.getState().get() == AbstractPoolableObject.IN_USE_STATE }).allMatch({ item -> item.refCnt() == 1 })
        Assertions.assertThat(releaseResults.subList(0, 3)).allMatch({ item -> item })
        objectPool.@firstObject.getState().get() == AbstractPoolableObject.IN_USE_STATE
        objectPool.@secondObject.getState().get() == AbstractPoolableObject.IN_USE_STATE
        Assertions.
                assertThat(objectPool.@objectArray).
                haveExactly(5 - 3, new Condition<? extends AbstractPoolableObject>({ item -> item == null }, "Is null")).
                haveExactly(objectPool.@objectArray.length - 5 + 3,
                            new Condition<? extends AbstractPoolableObject>({ item -> item?.getState()?.get() == AbstractPoolableObject.AVAILABLE_STATE }, "Is in available state"))
        objectPool.@objGetPosition == 16 + 5
        objectPool.@objectPutPosition == 16 + 16 + 3
    }

    def "should throw exception when releasing already released object"() {
        setup:
        def objectFromPool = objectPool.getAndRetain()
        objectFromPool.release()

        when:
        objectFromPool.release()

        then:
        thrown(IllegalStateException)
    }

    def "should resize pool once it's out of objects"() {
        setup:
        def initialPoolSize = objectPool.@objectArray.length
        (initialPoolSize + 2).times {
            objectPool.getAndRetain()
        }

        when:
        def objectFromPool = objectPool.getAndRetain()

        then:
        objectFromPool != null
        objectPool.@objectArray.length == initialPoolSize << 1
        Assertions.
                assertThat(objectPool.@objectArray).
                haveExactly(initialPoolSize + 1, new Condition<? extends AbstractPoolableObject>({ item -> item == null }, "Is null")).
                haveExactly(objectPool.@objectArray.length - initialPoolSize - 1, new Condition<? extends AbstractPoolableObject>({ item -> item != null }, "Is not null"))
        Modifier.isFinal(objectPool.getClass().getSuperclass().getDeclaredField("mask").getModifiers())
        Modifier.isFinal(objectPool.getClass().getSuperclass().getDeclaredField("objectArray").getModifiers())
    }

    def "should throw illegal state exception when returning object in wrong state"() {
        setup:
        def poolableObject = objectPool.getAndRetain()
        poolableObject.getState().set(AbstractPoolableObject.AVAILABLE_STATE)

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
        objectPool.@firstObject.closeInvoked
        objectPool.@secondObject.closeInvoked
        Assertions.assertThat(objectPool.@objectArray).containsOnlyNulls()
        Assertions.assertThat(objectsInPool).allMatch({ it -> it.closeInvoked })
    }

    def "should check if all objects are returned to the pool"() {
        setup:
        objectPool.@firstObject.getState().set(firstObjectState)
        objectPool.@secondObject.getState().set(secondObjectState)
        objectArrayConsumer.accept(objectPool.@objectArray)

        expect:
        objectPool.areAllObjectsReturned() == expectedResult

        where:
        firstObjectState                       | secondObjectState                      | objectArrayConsumer                                                                                        | expectedResult
        AbstractPoolableObject.AVAILABLE_STATE | AbstractPoolableObject.AVAILABLE_STATE | (Consumer<AbstractPoolableObject[]>) { arr -> }                                                            | true
        AbstractPoolableObject.AVAILABLE_STATE | AbstractPoolableObject.AVAILABLE_STATE | (Consumer<AbstractPoolableObject[]>) { arr -> arr[0] = null }                                              | false
        AbstractPoolableObject.AVAILABLE_STATE | AbstractPoolableObject.AVAILABLE_STATE | (Consumer<AbstractPoolableObject[]>) { arr -> arr[0].getState().set(AbstractPoolableObject.IN_USE_STATE) } | false
        AbstractPoolableObject.AVAILABLE_STATE | AbstractPoolableObject.IN_USE_STATE    | (Consumer<AbstractPoolableObject[]>) { arr -> arr[0] = null }                                              | false
        AbstractPoolableObject.IN_USE_STATE    | AbstractPoolableObject.AVAILABLE_STATE | (Consumer<AbstractPoolableObject[]>) { arr -> arr[0] = null }                                              | false
    }

    static class TestPoolableObject extends AbstractPoolableObject {

        boolean closeInvoked = false

        @Override
        void close() {
            closeInvoked = true
        }
    }
}
