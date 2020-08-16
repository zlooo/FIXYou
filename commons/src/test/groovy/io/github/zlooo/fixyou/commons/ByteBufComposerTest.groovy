package io.github.zlooo.fixyou.commons

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions
import spock.lang.Specification

class ByteBufComposerTest extends Specification {

    public static final ByteBufComposer.Component EMPTY_COMPONENT = new ByteBufComposer.Component()
    private ByteBufComposer composer = new ByteBufComposer(10)

    def "should add buffer"() {
        setup:
        def bufferToAdd = Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[])

        when:
        composer.addByteBuf(bufferToAdd)

        then:
        composer.writeComponentIndex == 1
        Assertions.assertThat(composer.components.toList().subList(1, 10).collect { it.buffer }).containsOnlyNulls()
        def component = composer.components[0]
        component.buffer == bufferToAdd
        component.buffer.refCnt() == 2
        component.startIndex == 0
        component.endIndex == 4
        composer.storedStartIndex == 0
        composer.storedEndIndex == 4
    }

    def "should add next buffer"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[]))
        def bufferToAdd = Unpooled.wrappedBuffer([6, 7] as byte[])

        when:
        composer.addByteBuf(bufferToAdd)

        then:
        composer.writeComponentIndex == 2
        Assertions.assertThat(composer.components.toList().subList(2, 10).collect { it.buffer }).containsOnlyNulls()
        def component = composer.components[1]
        component.buffer == bufferToAdd
        component.buffer.refCnt() == 2
        component.startIndex == 5
        component.endIndex == 6
        composer.storedStartIndex == 0
        composer.storedEndIndex == 6
    }

    def "should add, release and add second buffer"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[]))
        composer.releaseData(0, 4)
        def bufferToAdd = Unpooled.wrappedBuffer([6, 7] as byte[])

        when:
        composer.addByteBuf(bufferToAdd)

        then:
        composer.writeComponentIndex == 1
        Assertions.assertThat(composer.components.toList().subList(1, 10).collect { it.buffer }).containsOnlyNulls()
        def component = composer.components[0]
        component.buffer == bufferToAdd
        component.buffer.refCnt() == 2
        component.startIndex == 0
        component.endIndex == 1
        composer.storedStartIndex == 0
        composer.storedEndIndex == 1
    }

    def "should not add more data than buffer capacity"() {
        setup:
        def buffers = (0..composer.components.length).collect { Unpooled.wrappedBuffer([it] as byte[]) }
        buffers.subList(0, composer.components.length).forEach { composer.addByteBuf(it) }

        when:
        composer.addByteBuf(buffers.last())

        then:
        thrown(BufferFullException)
    }

    def "should release whole buffer"() {
        setup:
        def bufferToAdd = Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[])
        def bufferToAdd2 = Unpooled.wrappedBuffer([6, 7] as byte[])
        composer.addByteBuf(bufferToAdd)
        composer.addByteBuf(bufferToAdd2)
        composer.readerIndex(2)

        when:
        composer.releaseData(0, index)

        then:
        composer.storedStartIndex == -1
        composer.storedEndIndex == -1
        composer.writeComponentIndex == 0
        composer.readerIndex == 0
        Assertions.assertThat(composer.components).containsOnly(EMPTY_COMPONENT)
        bufferToAdd.refCnt() == 1
        bufferToAdd2.refCnt() == 1

        where:
        index | _
        6     | _
        7     | _
        10    | _
        50    | _
        666   | _
    }

    def "should not allow to leave holes after release"() {
        setup:
        def bufferToAdd = Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[])
        composer.addByteBuf(bufferToAdd)

        when:
        composer.releaseData(startIndex, endIndex)

        then:
        thrown(IllegalArgumentException)

        where:
        startIndex | endIndex
        1          | 1
        1          | 2
        1          | 3
        2          | 2
        2          | 3
        3          | 3
    }

    def "should release beginning of component"() {
        setup:
        def bufferToAdd1 = Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[])
        def bufferToAdd2 = Unpooled.wrappedBuffer([6, 7, 8, 9, 10] as byte[])
        def bufferToAdd3 = Unpooled.wrappedBuffer([11, 12, 13, 14, 15] as byte[])
        composer.addByteBuf(bufferToAdd1)
        composer.addByteBuf(bufferToAdd2)
        composer.addByteBuf(bufferToAdd3)

        when:
        composer.releaseData(5, endIndex)

        then:
        composer.components[0].startIndex == 0
        composer.components[0].endIndex == 4
        composer.components[1].startIndex == endIndex+1
        composer.components[1].endIndex == 9
        composer.components[2].startIndex == 10
        composer.components[2].endIndex == 14
        composer.storedStartIndex == 0
        composer.storedEndIndex == 14
        bufferToAdd1.refCnt() == 2
        bufferToAdd2.refCnt() == 2
        bufferToAdd3.refCnt() == 2

        where:
        endIndex | _
        5        | _
        6        | _
        7        | _
        8        | _
    }

    def "should release whole component"() {
        setup:
        def bufferToAdd1 = Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[])
        def bufferToAdd2 = Unpooled.wrappedBuffer([6, 7, 8, 9, 10] as byte[])
        def bufferToAdd3 = Unpooled.wrappedBuffer([11, 12, 13, 14, 15] as byte[])
        composer.addByteBuf(bufferToAdd1)
        composer.addByteBuf(bufferToAdd2)
        composer.addByteBuf(bufferToAdd3)

        when:
        composer.releaseData(5, 9)

        then:
        composer.components[0].startIndex == 0
        composer.components[0].endIndex == 4
        composer.components[1].startIndex == ByteBufComposer.INITIAL_VALUE
        composer.components[1].endIndex == ByteBufComposer.INITIAL_VALUE
        composer.components[2].startIndex == 10
        composer.components[2].endIndex == 14
        composer.storedStartIndex == 0
        composer.storedEndIndex == 14
        bufferToAdd1.refCnt() == 2
        bufferToAdd2.refCnt() == 1
        bufferToAdd3.refCnt() == 2
    }

    def "should release couple of consecutive components"() {
        setup:
        def bufferToAdd1 = Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[])
        def bufferToAdd2 = Unpooled.wrappedBuffer([6, 7, 8, 9, 10] as byte[])
        def bufferToAdd3 = Unpooled.wrappedBuffer([11, 12, 13, 14, 15] as byte[])
        composer.addByteBuf(bufferToAdd1)
        composer.addByteBuf(bufferToAdd2)
        composer.addByteBuf(bufferToAdd3)

        when:
        composer.releaseData(0, 12)

        then:
        composer.components[0].startIndex == ByteBufComposer.INITIAL_VALUE
        composer.components[0].endIndex == ByteBufComposer.INITIAL_VALUE
        composer.components[1].startIndex == ByteBufComposer.INITIAL_VALUE
        composer.components[1].endIndex == ByteBufComposer.INITIAL_VALUE
        composer.components[2].startIndex == 13
        composer.components[2].endIndex == 14
        composer.storedStartIndex == 13
        composer.storedEndIndex == 14
        bufferToAdd1.refCnt() == 1
        bufferToAdd2.refCnt() == 1
        bufferToAdd3.refCnt() == 2
    }

    def "should not get released part of buffer"(){
        setup:
        def bufferToAdd1 = Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[])
        def bufferToAdd2 = Unpooled.wrappedBuffer([6, 7, 8, 9, 10] as byte[])
        def bufferToAdd3 = Unpooled.wrappedBuffer([11, 12, 13, 14, 15] as byte[])
        composer.addByteBuf(bufferToAdd1)
        composer.addByteBuf(bufferToAdd2)
        composer.addByteBuf(bufferToAdd3)
        composer.releaseData(5, 9)

        when:
        composer.getByte(6)

        then:
        thrown(IndexOutOfBoundsException)
    }

    def "should get bytes after buffer overlap"() {
        setup:
        List<ByteBuf> buffers = []
        (0..9).forEach { buffers.add(Unpooled.wrappedBuffer([it] as byte[])) }
        buffers.forEach { composer.addByteBuf(it) }
        composer.releaseData(0, 4)
        (10..13).forEach { buffers.add(Unpooled.wrappedBuffer([it] as byte[])) }
        buffers.subList(10, buffers.size()).forEach { composer.addByteBuf(it) }
        def destination = new byte[10]

        when:
        composer.getBytes(8, 3, destination)

        then:
        Assertions.assertThat(destination).containsExactly(resize([8, 9, 10] as byte[], destination.length))
    }

    def "should get bytes"() {
        setup:
        def bytes = [1, 2, 3, 4, 5] as byte[]
        composer.addByteBuf(Unpooled.wrappedBuffer(bytes))
        def destination = new byte[10]

        when:
        composer.getBytes(0, bytes.length, destination)

        then:
        Assertions.assertThat(destination).containsExactly(resize(bytes, destination.length))
    }

    def "should get bytes from two buffers"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([6, 7, 8] as byte[]))
        def destination = new byte[10]

        when:
        composer.getBytes(0, 8, destination)

        then:
        Assertions.assertThat(destination).containsExactly(resize([1, 2, 3, 4, 5, 6, 7, 8] as byte[], destination.length))
    }

    def "should get bytes from three buffers"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 3] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([4, 5] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([6, 7, 8] as byte[]))
        def destination = new byte[10]

        when:
        composer.getBytes(0, 8, destination)

        then:
        Assertions.assertThat(destination).containsExactly(resize([1, 2, 3, 4, 5, 6, 7, 8] as byte[], destination.length))
    }

    def "should get bytes from non zero index buffers"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 3] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([4, 5] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([6, 7, 8] as byte[]))
        def destination = new byte[10]

        when:
        composer.getBytes(4, 3, destination)

        then:
        Assertions.assertThat(destination).containsExactly(resize([5, 6, 7] as byte[], destination.length))
    }

    def "should get bytes from middle of component buffers"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([6, 7, 8] as byte[]))
        def destination = new byte[10]

        when:
        composer.getBytes(2, 6, destination)

        then:
        Assertions.assertThat(destination).containsExactly(resize([3, 4, 5, 6, 7, 8] as byte[], destination.length))
    }

    def "should not get bytes because of wrong index or length requested"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([6, 7, 8] as byte[]))
        def destination = new byte[10]

        when:
        composer.getBytes(index, length, destination)

        then:
        thrown(IndexOutOfBoundsException)

        where:
        index | length
        -1    | 1
        8     | 1
        0     | 9
        0     | 10
    }

    def "should get byte"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([6, 7, 8] as byte[]))

        expect:
        composer.getByte(index) == expectedResult

        where:
        index | expectedResult
        0     | 1 as byte
        1     | 2 as byte
        2     | 3 as byte
        3     | 4 as byte
        4     | 5 as byte
        5     | 6 as byte
        6     | 7 as byte
        7     | 8 as byte
    }

    def "should find index of closest for single buffer"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[]))

        expect:
        composer.indexOfClosest(elementToFind) == expectedResult

        where:
        elementToFind | expectedResult
        1 as byte     | 0
        3 as byte     | 2
        5 as byte     | 4
        11 as byte    | ByteBufComposer.VALUE_NOT_FOUND
        128 as byte   | ByteBufComposer.VALUE_NOT_FOUND
    }

    def "should find index of closest"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 3, 4, 5] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([6, 7, 8] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([9, 10] as byte[]))

        expect:
        composer.indexOfClosest(elementToFind) == expectedResult

        where:
        elementToFind | expectedResult
        1 as byte     | 0
        3 as byte     | 2
        5 as byte     | 4
        6 as byte     | 5
        7 as byte     | 6
        8 as byte     | 7
        9 as byte     | 8
        10 as byte    | 9
        11 as byte    | ByteBufComposer.VALUE_NOT_FOUND
        128 as byte   | ByteBufComposer.VALUE_NOT_FOUND
    }

    def "should find index of closest when reader index is moved"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 1, 2, 3] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 3] as byte[]))
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 4] as byte[]))
        composer.readerIndex(readerIndex)

        expect:
        composer.indexOfClosest(elementToFind) == expectedResult

        where:
        elementToFind | readerIndex | expectedResult
        1 as byte     | 0           | 0
        1 as byte     | 1           | 2
        1 as byte     | 2           | 2
        3 as byte     | 3           | 4
        1 as byte     | 3           | 5
        2 as byte     | 4           | 6
        4 as byte     | 2           | 9
    }

    def "should reset composer"() {
        setup:
        composer.addByteBuf(Unpooled.wrappedBuffer([1, 2, 1, 2, 3] as byte[]))
        composer.readerIndex(2)

        when:
        composer.reset()

        then:
        composer.writeComponentIndex == 0;
        composer.storedStartIndex == -1;
        composer.storedEndIndex == -1;
        composer.readerIndex == 0;
        Assertions.assertThat(composer.components).containsOnly(EMPTY_COMPONENT)
    }

    private byte[] resize(byte[] source, length) {
        byte[] array = new byte[length]
        System.arraycopy(source, 0, array, 0, source.length)
        return array
    }

    List components(int startingIndex, int endingIndex) {
        (startingIndex..endingIndex).collect { index -> new ByteBufComposer.Component(startIndex: index, endIndex: index, buffer: Unpooled.wrappedBuffer([index] as byte[])) }
    }
}
