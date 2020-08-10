package io.github.zlooo.fixyou.commons

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.assertj.core.api.Assertions
import spock.lang.Specification

class ByteBufComposerTest extends Specification {

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

        when:
        composer.releaseDataUpTo(index)

        then:
        composer.storedStartIndex == -1
        composer.storedEndIndex == -1
        composer.writeComponentIndex == 0
        Assertions.assertThat(composer.components).containsOnly(new ByteBufComposer.Component())
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

    def "should release part of buffer"() {
        setup:
        List<ByteBuf> buffers = []
        (0..9).forEach { buffers.add(Unpooled.wrappedBuffer([it] as byte[])) }
        buffers.forEach { composer.addByteBuf(it) }

        when:
        composer.releaseDataUpTo(index)

        then:
        composer.storedStartIndex == index + 1
        composer.storedEndIndex == buffers.size() - 1
        composer.writeComponentIndex == 0
        Assertions.assertThat(composer.components.toList().subList(0, index + 1)).containsOnly(new ByteBufComposer.Component())
        Assertions.assertThat(composer.components.toList().subList(index + 1, buffers.size())).containsExactlyElementsOf(components(index + 1, buffers.size() - 1))
        buffers.subList(0, index + 1).every { it.refCnt() == 1 }

        where:
        index << (0..8).toList()
    }

    def "should get bytes after buffer overlap"() {
        setup:
        List<ByteBuf> buffers = []
        (0..9).forEach { buffers.add(Unpooled.wrappedBuffer([it] as byte[])) }
        buffers.forEach { composer.addByteBuf(it) }
        composer.releaseDataUpTo(4)
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
        1 as byte     | 3           | 5
        2 as byte     | 4           | 6
        4 as byte     | 2           | 9
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
