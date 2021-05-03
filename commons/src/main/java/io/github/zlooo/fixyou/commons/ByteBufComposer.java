package io.github.zlooo.fixyou.commons;

import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.util.ByteProcessor;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class quite similar in concept to {@link CompositeByteBuf} but tailored to FIXYou needs. It also acts as a view that composes multiple {@link ByteBuf} into single, continuous stream of bytes. Main difference is that once component is
 * removed this class reader index remains unchanged and points to the same place. This means that it's possible to have a situation where this class in unable to provide data for given reader index. Let's imagine a situation where
 * component 1 holds 5 bytes, component 2 holds 10 bytes. If a reader index is 6, data will be read from component 2 second byte, same as with {@link CompositeByteBuf}. However if composite 1 is removed before this theoretical read this
 * class will read the same data, {@link CompositeByteBuf} would read 6th byte of composite 2 instead. Generally speaking this is <B>NOT</B> a general purpose class so you use it in your code at your own risk.
 */
@Slf4j
@ToString
public class ByteBufComposer implements Resettable {

    public static final int NOT_FOUND = -1;
    private static final int INITIAL_VALUE = -1;
    private static final String IOOBE_MESSAGE = "This instance does not contain data for index ";
    private static final ByteProcessor SOH_FINDER = new ByteProcessor.IndexOfProcessor((byte) 1);
    @ToString.Exclude
    private final Component[] components;
    private final IntIntAVLTree startIndexToComponentIndex;
    private final int mask;
    private int arrayIndex;
    private int readerIndex;
    @Getter
    private int storedStartIndex = INITIAL_VALUE;
    @ToString.Exclude
    private int p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15;
    @Getter
    private int storedEndIndex = INITIAL_VALUE;

    public ByteBufComposer(int initialNumberOfComponents) {
        int currentSize = 2;
        while (currentSize < initialNumberOfComponents) {
            currentSize = currentSize << 1;
        }
        this.components = new Component[currentSize];
        for (int i = 0; i < components.length; i++) {
            components[i] = new Component();
        }
        this.startIndexToComponentIndex = new IntIntAVLTree(currentSize);
        mask = currentSize - 1;
    }

    public boolean addByteBuf(ByteBuf byteBuf) {
        final Component previousComponent = ArrayUtils.getElementAt(components, toArrayIndex(arrayIndex - 1));
        final int currentComponentArrayIndex = toArrayIndex(arrayIndex++);
        final Component component = ArrayUtils.getElementAt(components, currentComponentArrayIndex);
        if (component.buffer != null) {
            return false;
        }
        if (previousComponent.endIndex != INITIAL_VALUE) { //TODO JMH this and check if changing this to a boolean variable improves performance
            component.startIndex = previousComponent.endIndex + 1;
        } else {
            component.startIndex = 0;
            storedStartIndex = 0;
        }
        startIndexToComponentIndex.put(component.startIndex, currentComponentArrayIndex);
        component.offset = component.startIndex;
        component.endIndex = component.startIndex + byteBuf.readableBytes() - 1;
        storedEndIndex = component.endIndex;
        byteBuf.retain();
        component.buffer = byteBuf;
        return true;
    }

    public void releaseData(int startIndexInclusive, int endIndexInclusive) {
        log.trace("Releasing indexes <{}, {}>", startIndexInclusive, endIndexInclusive);
        if (startIndexInclusive > endIndexInclusive) {
            throw new IllegalArgumentException("Start index(" + startIndexInclusive + ") bigger than end index(" + endIndexInclusive + ")? No can do");
        }
        if (coversWholeBuffer(startIndexInclusive, endIndexInclusive)) {
            reset();
        } else {
            int componentArrayIndex = findReaderComponentIndex(Math.max(startIndexInclusive, storedStartIndex)); //here we're making sure that startIndexInclusive is between this component's start and end index
            Component component;
            for (; ; ) {
                component = ArrayUtils.getElementAt(components, toArrayIndex(componentArrayIndex++));
                if (isEmpty(component)) {
                    break;
                }
                final boolean endEqualsOrExceedsComponentsEnd = component.endIndex <= endIndexInclusive;
                final boolean startEqualsOrPrecedesComponentsStart = startIndexInclusive <= component.startIndex;
                if (!(startEqualsOrPrecedesComponentsStart || endEqualsOrExceedsComponentsEnd)) {
                    throw new IllegalArgumentException(
                            String.format("No holes allowed, after releasing <%1$d, %2$d> from <%3$d, %4$d> we'd have 2 ranges in single component", startIndexInclusive, endIndexInclusive, component.startIndex, component.endIndex));
                }
                if (endEqualsOrExceedsComponentsEnd && startEqualsOrPrecedesComponentsStart) {
                    component.reset();
                    startIndexToComponentIndex.remove(component.startIndex);
                } else {
                    if (startEqualsOrPrecedesComponentsStart) {
                        final int componentIndex = startIndexToComponentIndex.remove(component.startIndex);
                        component.startIndex = endIndexInclusive + 1;
                        startIndexToComponentIndex.put(component.startIndex, componentIndex);
                    } else {
                        component.endIndex = startIndexInclusive - 1;
                    }
                    break;
                }
            }
            updateStoredIndexes(startIndexInclusive, endIndexInclusive);
        }
    }

    private void updateReaderIndexIfNecessary(int startIndexInclusive, int endIndexInclusive) {
        if (readerIndex < storedStartIndex) {
            readerIndex = storedStartIndex;
        }
        if (startIndexInclusive <= readerIndex && readerIndex <= endIndexInclusive) {
            readerIndex = endIndexInclusive + 1;
        }
    }

    //TODO this is an easy, brute force approach :/ think about something better
    private void updateStoredIndexes(int startIndexInclusive, int endIndexInclusive) {
        int minStartIndex = Integer.MAX_VALUE;
        boolean minimumFound = false;
        for (final Component component : components) {
            final int startIndex = component.startIndex;
            if (startIndex != INITIAL_VALUE && startIndex < minStartIndex) {
                minStartIndex = startIndex;
                minimumFound = true;
            }
        }
        if (minimumFound) {
            storedStartIndex = minStartIndex;
            updateReaderIndexIfNecessary(startIndexInclusive, endIndexInclusive);
        } else {
            resetAllButComponents();
        }
    }

    private boolean coversWholeBuffer(int startIndexInclusive, int endIndexInclusive) {
        return storedStartIndex >= startIndexInclusive && storedEndIndex <= endIndexInclusive;
    }

    private static boolean isEmpty(Component component) {
        return component.buffer == null;
    }

    private int toArrayIndex(int index) {
        return index & mask;
    }

    public void getBytes(int index, int length, ByteBuf destination) {
        checkIndex(index, length);
        int readerComponentIndex = findReaderComponentIndex(index);
        int remainingBytesToRead = length;
        int bytesRead = 0;
        int localReaderIndex = index;
        Component component;
        while (remainingBytesToRead > 0) {
            component = ArrayUtils.getElementAt(components, toArrayIndex(readerComponentIndex++));
            final int bytesReadFromComponent = readDataFromComponent(component, localReaderIndex, remainingBytesToRead, destination, bytesRead);
            bytesRead += bytesReadFromComponent;
            localReaderIndex += bytesReadFromComponent;
            remainingBytesToRead -= bytesReadFromComponent;
        }
    }

    private void checkIndex(int index, int length) {
        if (index < storedStartIndex || index > storedEndIndex) {
            throw new ByteBufComposerIndexOutOfBoundsException(IOOBE_MESSAGE + index, this);
        }
        final int requestedEndIndex = index + length - 1;
        if (requestedEndIndex < storedStartIndex || requestedEndIndex > storedEndIndex) {
            throw new ByteBufComposerIndexOutOfBoundsException(IOOBE_MESSAGE + requestedEndIndex, this);
        }
    }

    public byte getByte(int index) {
        final Component component = ArrayUtils.getElementAt(components, findReaderComponentIndex(index));
        return component.getBuffer().getByte(index - component.offset);
    }

    private int findReaderComponentIndex(int index) {
        final int floorValue = startIndexToComponentIndex.floorValue(index);
        if (floorValue != IntIntAVLTree.INVALID_IDX && ArrayUtils.getElementAt(components, floorValue).getEndIndex() >= index) {
            return floorValue;
        }
        throw new ByteBufComposerIndexOutOfBoundsException(IOOBE_MESSAGE + index, this);
    }

    private int readDataFromComponent(Component component, int index, int maxLength, ByteBuf destination, int destinationIndex) {
        final int startIndex = index - component.offset;
        final ByteBuf buffer = component.buffer;
        final int numberOfBytesToTransfer = Math.min(maxLength, buffer.writerIndex() - startIndex);
        buffer.getBytes(startIndex, destination, destinationIndex, numberOfBytesToTransfer);
        destination.writerIndex(destinationIndex + numberOfBytesToTransfer);
        return numberOfBytesToTransfer;
    }

    @Override
    public void reset() {
        resetAllButComponents();
        for (final Component component : components) {
            component.reset();
        }
    }

    private void resetAllButComponents() {
        arrayIndex = 0;
        readerIndex = 0;
        storedStartIndex = INITIAL_VALUE;
        storedEndIndex = INITIAL_VALUE;
        startIndexToComponentIndex.clear();
    }

    public int readerIndex() {
        return readerIndex;
    }

    public void readerIndex(int newReaderIndex) {
        this.readerIndex = newReaderIndex;
    }

    public boolean readerIndexBeyondStoredEnd() {
        return readerIndex >= storedEndIndex;
    }

    public int indexOfClosestSOH() {
        return indexOfClosest(SOH_FINDER);
    }

    /**
     * Finds closest index of provided value that's greater than current reader index
     *
     * @param valueToFind value to look for, should be instance of {@link io.netty.util.ByteProcessor.IndexOfProcessor} but any {@link ByteProcessor} will be accepted, it's up to you to make sure it's correct one
     * @return index of given value or {@link #NOT_FOUND}
     */
    public int indexOfClosest(ByteProcessor valueToFind) {
        int readerComponentIndex = findReaderComponentIndex(readerIndex);
        while (true) {
            final Component component = ArrayUtils.getElementAt(components, toArrayIndex(readerComponentIndex++));
            if (!isEmpty(component) && component.endIndex >= readerIndex) {
                final int fromIndex = Math.max(readerIndex - component.offset, 0);
                /**
                 * According to {@link io.github.zlooo.fixyou.commons.ByteBufComposerPerformanceTest} {@link ByteBuf#forEachByte(int, int, ByteProcessor)} is almost 4,5 times faster than {@link ByteBuf#indexOf(int, int, byte)} that's
                 * quite a difference
                 */
                final int result = component.buffer.forEachByte(fromIndex, component.buffer.writerIndex() - fromIndex, valueToFind);
                if (result != NOT_FOUND) {
                    return result + component.offset;
                }
            } else {
                break;
            }
        }
        return NOT_FOUND;
    }

    @ToString.Include
    private String components() {
        return "(Only non-empty)" + Stream.of(components).filter(item -> item.buffer != null).map(Component::toString).collect(Collectors.joining(",\n"));
    }

    @Data
    @ToString
    static final class Component implements Resettable {
        private int startIndex = INITIAL_VALUE;
        private int endIndex = INITIAL_VALUE;
        private int offset;
        @ToString.Exclude
        private ByteBuf buffer;

        @Override
        public void reset() {
            if (buffer != null) {
                buffer.release();
                buffer = null;
            }
            startIndex = INITIAL_VALUE;
            endIndex = INITIAL_VALUE;
            offset = 0;
        }

        @ToString.Include
        private String buffer() {
            if (buffer != null) {
                return buffer.toString() + "containing: " + buffer.toString(0, buffer.writerIndex(), StandardCharsets.US_ASCII);
            } else {
                return null;
            }
        }
    }
}
