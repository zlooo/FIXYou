package io.github.zlooo.fixyou.commons.memory;

import io.github.zlooo.fixyou.Resettable;
import io.github.zlooo.fixyou.commons.pool.AbstractPoolableObject;
import io.github.zlooo.fixyou.utils.UnsafeAccessor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class Region extends AbstractPoolableObject implements Resettable {

    public static final long NO_SPACE_AVAILABLE = -1;
    private static final short MODULO_8_MASK = 7;
    private final long startingAddress;
    private final short size;
    @Getter(AccessLevel.NONE)
    private int bytesTaken;

    public Region(long startingAddress, short size) {
        this.startingAddress = startingAddress;
        this.size = size;
    }

    @Override
    public void close() {
        //nothing to do
    }

    @Override
    public void reset() {
        bytesTaken = 0;
    }

    @Override
    protected void deallocate() {
        reset();
        super.deallocate();
    }

    public long append(short numberOfBytes) {
        final int newTakenSize = bytesTaken + numberOfBytes;
        if (newTakenSize > this.size) {
            return NO_SPACE_AVAILABLE;
        } else {
            final long address = startingAddress + bytesTaken;
            bytesTaken = newTakenSize;
            return address;
        }
    }

    public void copyDataFrom(Region source) {
        final int bytesToCopy = size - (size & MODULO_8_MASK);
        UnsafeAccessor.UNSAFE.copyMemory(source.startingAddress, startingAddress, bytesToCopy);
        UnsafeAccessor.UNSAFE.copyMemory(source.startingAddress + bytesToCopy, startingAddress + bytesToCopy, size - bytesToCopy);
    }
}
