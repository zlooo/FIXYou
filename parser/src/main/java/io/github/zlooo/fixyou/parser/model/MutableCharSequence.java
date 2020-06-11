package io.github.zlooo.fixyou.parser.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
class MutableCharSequence implements CharSequence {

    public static final String RANGE_EXCEPTION_MESSAGE_PART = " is outside of this CharSequence range, length=";
    private char[] state;
    private int length;

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        if (index < length) {
            return state[index];
        } else {
            throw new IndexOutOfBoundsException("Index " + index + RANGE_EXCEPTION_MESSAGE_PART + length);
        }
    }

    /**
     * Do not use if you care about amount of garbage your application produces
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        if (start < 0 || end <= length) {
            final MutableCharSequence subSequence = new MutableCharSequence();
            final int subSequenceLength = end - start;
            subSequence.length = subSequenceLength;
            subSequence.state = new char[subSequenceLength];
            System.arraycopy(state, start, subSequence.state, 0, subSequenceLength);
            return subSequence;
        } else {
            throw new IndexOutOfBoundsException("Requested sub sequence with indexes start=" + start + ", end=" + end + RANGE_EXCEPTION_MESSAGE_PART + length);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(state, 0, length);
    }
}
