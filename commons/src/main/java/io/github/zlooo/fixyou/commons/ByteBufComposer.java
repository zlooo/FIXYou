package io.github.zlooo.fixyou.commons;

import io.github.zlooo.fixyou.Resettable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;

/**
 * Class quite similar in concept to {@link CompositeByteBuf}. It also acts as a view that composes multiple {@link ByteBuf} into single, continuous stream of bytes. Main difference is that once component is removed this class reader
 * index remains unchanged and points to the same place. This means that it's possible to have a situation where this class in unable to provide data for given reader index. Let's imagine a situation where component 1 holds 5 bytes,
 * component 2 holds 10 bytes. If a reader index is 6, data will be read from component 2 second byte, same as with {@link CompositeByteBuf}. However if composite 1 is removed before this theoretical read this class will read the same
 * data, {@link CompositeByteBuf} would read 6th byte of composite 2 instead
 */
public class ByteBufComposer implements Resettable {

    private static final int VALUE_NOT_FOUND = Integer.MIN_VALUE;
    private static final String IOOBE_MESSAGE = "This instance does not contain data for index ";
    private final Component[] components;
    private int writeComponentIndex;
    private int storedStartIndex = -1;
    private int storedEndIndex = -1;

    public ByteBufComposer(int initialNumberOfComponents) {
        this.components = new Component[initialNumberOfComponents];
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

    private int previousComponentIndex(int componentIndex) {
        if (componentIndex - 1 < 0) {
            return components.length - 1;
        } else {
            return componentIndex - 1;
        }
    }

    public void readBytes(int index, int length, byte[] destination) {
        checkIndex(index, length);
        int readerComponentIndex = VALUE_NOT_FOUND;
        for (int i = 0; i < components.length; i++) {
            final Component component = components[i];
            if (component.endIndex >= index && component.startIndex <= index) {
                readerComponentIndex = i - 1;
                break;
            }
        }
        int remainingBytesToRead = length;
        int bytesRead = 0;
        int readerIndex = index;
        Component component;
        while (remainingBytesToRead > 0) {
            readerComponentIndex = nextComponentIndex(readerComponentIndex);
            component = components[readerComponentIndex];
            final int bytesReadFromComponent = readDataFromComponent(component, readerIndex, remainingBytesToRead, destination, bytesRead);
            bytesRead += bytesReadFromComponent;
            readerIndex += bytesReadFromComponent;
            remainingBytesToRead -= bytesReadFromComponent;
        }
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

        public int getStartIndex() {
            return this.startIndex;
        }

        public int getEndIndex() {
            return this.endIndex;
        }

        public ByteBuf getBuffer() {
            return this.buffer;
        }

        public void setStartIndex(int startIndex) {
            this.startIndex = startIndex;
        }

        public void setEndIndex(int endIndex) {
            this.endIndex = endIndex;
        }

        public void setBuffer(ByteBuf buffer) {
            this.buffer = buffer;
        }

        public boolean equals(final Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Component))
                return false;
            final Component other = (Component) o;
            if (this.getStartIndex() != other.getStartIndex())
                return false;
            if (this.getEndIndex() != other.getEndIndex())
                return false;
            final Object this$buffer = this.getBuffer();
            final Object other$buffer = other.getBuffer();
            if (this$buffer == null ? other$buffer != null : !this$buffer.equals(other$buffer))
                return false;
            return true;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + this.getStartIndex();
            result = result * PRIME + this.getEndIndex();
            final Object $buffer = this.getBuffer();
            result = result * PRIME + ($buffer == null ? 43 : $buffer.hashCode());
            return result;
        }

        public String toString() {
            return "ByteBufComposer.Component(startIndex=" + this.getStartIndex() + ", endIndex=" + this.getEndIndex() + ", buffer=" + this.getBuffer() + ")";
        }
    }
}
