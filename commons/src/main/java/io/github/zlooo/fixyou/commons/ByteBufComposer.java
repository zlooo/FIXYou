package io.github.zlooo.fixyou.commons;

import io.github.zlooo.fixyou.Resettable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import lombok.Data;
import lombok.Getter;

/**
 * Class quite similar in concept to {@link CompositeByteBuf}. It also acts as a view that composes multiple {@link ByteBuf} into single, continuous stream of bytes. Main difference is that once component is removed this class reader
 * index remains unchanged and points to the same place. This means that it's possible to have a situation where this class in unable to provide data for given reader index. Let's imagine a situation where component 1 holds 5 bytes,
 * component 2 holds 10 bytes. If a reader index is 6, data will be read from component 2 second byte, same as with {@link CompositeByteBuf}. However if composite 1 is removed before this theoretical read this class will read the same
 * data, {@link CompositeByteBuf} would read 6th byte of composite 2 instead
 */
public class ByteBufComposer implements Resettable {

    //        public static final int VALUE_NOT_FOUND = Integer.MIN_VALUE;
    public static final int NOT_FOUND = -1;
    private static final int INITIAL_VALUE = -1;
    private static final String IOOBE_MESSAGE = "This instance does not contain data for index ";
    private final Component[] components;
    private final int mask;
    private int arrayIndex;
    @Getter
    private int storedStartIndex = INITIAL_VALUE;
    @Getter
    private int storedEndIndex = INITIAL_VALUE;
    private int readerIndex;

    public ByteBufComposer(int initialNumberOfComponents) {
        int currentSize = 2;
        while (currentSize < initialNumberOfComponents) {
            currentSize = currentSize << 1;
        }
        this.components = new Component[currentSize];
        for (int i = 0; i < components.length; i++) {
            components[i] = new Component();
        }
        mask = currentSize - 1;
    }

    public void addByteBuf(ByteBuf byteBuf) {
        byteBuf.retain();
        final Component component = components[toArrayIndex(arrayIndex++)];
        if (component.buffer != null) {
            throw new BufferFullException();
        }
        component.buffer = byteBuf;
        if (storedStartIndex != INITIAL_VALUE) {
            component.startIndex = storedEndIndex + 1;
        } else {
            component.startIndex = 0;
            storedStartIndex = 0;
        }
        component.endIndex = component.startIndex + byteBuf.readableBytes() - 1;
        storedEndIndex = component.endIndex;
        component.buffer = byteBuf;
    }

    public void releaseData(int startIndexInclusive, int endIndexInclusive) {
        if (startIndexInclusive > endIndexInclusive) {
            throw new IllegalArgumentException("Start index(" + startIndexInclusive + ") bigger than end index(" + endIndexInclusive + ")? No can do");
        }
        if (coversWholeBuffer(startIndexInclusive, endIndexInclusive)) {
            reset();
        } else {
            int componentArrayIndex = findReaderComponentIndex(startIndexInclusive); //here we're making sure that startIndexInclusive is between this component's start and end index
            Component component;
            for (; ; ) {
                component = components[toArrayIndex(componentArrayIndex++)];
                if (isEmpty(component)) {
                    break;
                }
                if (component.endIndex <= endIndexInclusive) {
                    component.reset();
                } else {
                    component.startIndex = startIndexInclusive;
                    component.endIndex = endIndexInclusive;
                    break;
                }
            }
            updateStoredIndexes();
        }
    }

    //TODO this is an easy, brute force approach :/ think about something better
    private void updateStoredIndexes() {
        int maxEndIndex = Integer.MIN_VALUE;
        int minStartIndex = Integer.MAX_VALUE;
        for (final Component component : components) {
            final int endIndex = component.endIndex;
            final int startIndex = component.startIndex;
            if (endIndex > maxEndIndex) {
                maxEndIndex = endIndex;
            }
            if (startIndex < minStartIndex) {
                minStartIndex = startIndex;
            }
        }
        storedStartIndex = minStartIndex;
        storedEndIndex = maxEndIndex;
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

    public void getBytes(int index, int length, byte[] destination) {
        int readerComponentIndex = findReaderComponentIndex(index);
        int remainingBytesToRead = length;
        int bytesRead = 0;
        int localReaderIndex = index;
        Component component;
        while (remainingBytesToRead > 0) {
            component = components[toArrayIndex(readerComponentIndex++)];
            final int bytesReadFromComponent = readDataFromComponent(component, localReaderIndex, remainingBytesToRead, destination, bytesRead);
            bytesRead += bytesReadFromComponent;
            localReaderIndex += bytesReadFromComponent;
            remainingBytesToRead -= bytesReadFromComponent;
        }
    }

    public byte getByte(int index) {
        final Component component = components[findReaderComponentIndex(index)];
        return component.getBuffer().getByte(index - component.startIndex);
    }

    private int findReaderComponentIndex(int index) {
        for (int i = 0; i < components.length; i++) {
            final Component component = components[i];
            if (component.endIndex >= index && component.startIndex <= index) {
                return i;
            }
        }
        throw new IndexOutOfBoundsException(IOOBE_MESSAGE + index);
    }

    private int readDataFromComponent(Component component, int index, int maxLength, byte[] destination, int destinationIndex) {
        final int startIndex = index - component.startIndex;
        final ByteBuf buffer = component.buffer;
        final int numberOfBytesToTransfer = Math.min(maxLength, buffer.capacity() - startIndex);
        buffer.getBytes(startIndex, destination, destinationIndex, numberOfBytesToTransfer);
        return numberOfBytesToTransfer;
    }

    @Override
    public void reset() {
        arrayIndex = 0;
        readerIndex = 0;
        storedEndIndex = INITIAL_VALUE;
        for (final Component component : components) {
            component.reset();
        }
    }

    public int readerIndex() {
        return readerIndex;
    }

    public int readerIndex(int newReaderIndex) {
        this.readerIndex = newReaderIndex;
        return this.readerIndex;
    }

    /**
     * Finds closest index of provided value that's greater than current reader index
     *
     * @param valueToFind value to look for
     * @return index of given value or {@link #NOT_FOUND}
     */
    public int indexOfClosest(byte valueToFind) {
        int readerComponentIndex = findReaderComponentIndex(readerIndex);
        while (true) {
            final Component component = components[toArrayIndex(readerComponentIndex++)];
            if (!isEmpty(component) && component.endIndex >= readerIndex) {
                final int result = component.buffer.indexOf(readerIndex - component.startIndex, component.buffer.writerIndex(), valueToFind);
                if (result != NOT_FOUND) {
                    return result + component.startIndex;
                }
            } else {
                break;
            }
        }
        return NOT_FOUND;
    }

    @Data
    private static final class Component implements Resettable {
        private int startIndex = INITIAL_VALUE;
        private int endIndex = INITIAL_VALUE;
        private ByteBuf buffer;

        public Component() {
        }

        @Override
        public void reset() {
            if (buffer != null) {
                buffer.release();
                buffer = null;
            }
            startIndex = INITIAL_VALUE;
            endIndex = INITIAL_VALUE;
        }
    }
}
