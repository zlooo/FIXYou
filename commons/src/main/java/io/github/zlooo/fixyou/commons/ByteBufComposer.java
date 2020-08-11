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

    public static final int VALUE_NOT_FOUND = Integer.MIN_VALUE;
    private static final int NOT_FOUND = -1;
    private static final String IOOBE_MESSAGE = "This instance does not contain data for index ";
    private final Component[] components;
    private int writeComponentIndex;
    @Getter
    private int storedStartIndex = -1;
    @Getter
    private int storedEndIndex = -1;
    private int readerIndex;

    public ByteBufComposer(int initialNumberOfComponents) {
        this.components = new Component[initialNumberOfComponents == 1 ? 2 : initialNumberOfComponents]; //TODO I know it's a bit of a hack which I have to get rid of, it works but it fucks up my feng shui
        for (int i = 0; i < components.length; i++) {
            components[i] = new Component();
        }
    }

    public void addByteBuf(ByteBuf byteBuf) {
        byteBuf.retain();
        final Component component = components[writeComponentIndex];
        writeComponentIndex = nextComponentIndex(writeComponentIndex);
        if (component.buffer != null) {
            throw new BufferFullException();
        }
        component.buffer = byteBuf;
        if (storedEndIndex == -1) {
            storedStartIndex = 0;
            component.startIndex = 0;
        } else {
            component.startIndex = storedEndIndex + 1;
        }
        storedEndIndex += byteBuf.readableBytes();
        component.endIndex = storedEndIndex;
    }

    public void releaseDataUpTo(int indexInclusive) {
        if (indexInclusive < storedStartIndex) {
            throw new IndexOutOfBoundsException(IOOBE_MESSAGE + indexInclusive);
        }
        if (indexInclusive >= storedEndIndex) {
            reset();
        } else {
            storedStartIndex = indexInclusive + 1;
            for (final Component component : components) {
                if (component.endIndex <= indexInclusive) {
                    component.reset();
                }
            }
        }
    }

    private int nextComponentIndex(int componentIndex) {
        if (componentIndex + 1 >= components.length) {
            return 0;
        } else {
            return componentIndex + 1;
        }
    }

    public void getBytes(int index, int length, byte[] destination) {
        checkIndex(index, length);
        int readerComponentIndex = findReaderComponentIndex(index, true);
        int remainingBytesToRead = length;
        int bytesRead = 0;
        int localReaderIndex = index;
        Component component;
        while (remainingBytesToRead > 0) {
            readerComponentIndex = nextComponentIndex(readerComponentIndex);
            component = components[readerComponentIndex];
            final int bytesReadFromComponent = readDataFromComponent(component, localReaderIndex, remainingBytesToRead, destination, bytesRead);
            bytesRead += bytesReadFromComponent;
            localReaderIndex += bytesReadFromComponent;
            remainingBytesToRead -= bytesReadFromComponent;
        }
    }

    public byte getByte(int index) {
        checkIndex(index, 1);
        final Component component = components[findReaderComponentIndex(index, false)];
        return component.getBuffer().getByte(index - component.startIndex);
    }

    private int findReaderComponentIndex(int index, boolean decrement) {
        for (int i = 0; i < components.length; i++) {
            final Component component = components[i];
            if (component.endIndex >= index && component.startIndex <= index) {
                return decrement ? i - 1 : i;
            }
        }
        return VALUE_NOT_FOUND;
    }

    private void checkIndex(int index, int length) {
        if (index < storedStartIndex || index > storedEndIndex) {
            throw new IndexOutOfBoundsException(IOOBE_MESSAGE + index);
        }
        final int requestedEndIndex = index + length - 1;
        if (requestedEndIndex < storedStartIndex || requestedEndIndex > storedEndIndex) {
            throw new IndexOutOfBoundsException(IOOBE_MESSAGE + requestedEndIndex);
        }
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
        writeComponentIndex = 0;
        storedStartIndex = -1;
        storedEndIndex = -1;
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
     * @return index of given value or {@link #VALUE_NOT_FOUND}
     */
    public int indexOfClosest(byte valueToFind) {
        int readerComponentIndex = findReaderComponentIndex(readerIndex, true);
        while (true) {
            readerComponentIndex = nextComponentIndex(readerComponentIndex);
            final Component component = components[readerComponentIndex];
            if (component.buffer != null && component.endIndex >= readerIndex) {
                final int result = component.buffer.indexOf(readerIndex - component.startIndex, component.buffer.writerIndex(), valueToFind);
                if (result != NOT_FOUND) {
                    return result + component.startIndex;
                }
            } else {
                break;
            }
        }
        return VALUE_NOT_FOUND;
    }

    @Data
    private static final class Component implements Resettable {
        private int startIndex = -1;
        private int endIndex = -1;
        private ByteBuf buffer;

        public Component() {
        }

        @Override
        public void reset() {
            if (buffer != null) {
                buffer.release();
                buffer = null;
            }
            startIndex = -1;
            endIndex = -1;
        }
    }
}
