package io.github.zlooo.fixyou.commons;

import io.netty.util.AbstractReferenceCounted;
import io.netty.util.ReferenceCounted;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.annotation.Nonnull;

/**
 * Use with care, this class is potentially dangerous as it does not make a copy of state so if you use it make sure
 * array passed in {@link #setCharArray(char[])} is not modified afterwards. Also this class is
 * {@link io.netty.util.ReferenceCounted} so if obtained from pool make sure it's {@link #release()} method is called
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ReusableCharArray extends AbstractReferenceCounted implements CharSequence { //TODO get rid of this class!!!!

    private char[] state;
    private int length;

    @Nonnull
    public ReusableCharArray setCharArray(@Nonnull char[] array) {
        return setCharArray(array, array.length);
    }

    @Nonnull
    public ReusableCharArray setCharArray(@Nonnull char[] array, int charSequenceLength) {
        state = array;
        this.length = charSequenceLength;
        return this;
    }

    @Nonnull
    public char[] getCharArray() {
        return state;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return state[index];
    }

    @Override
    @Nonnull
    public CharSequence subSequence(int start, int end) {
        return new PortionView(start, end);
    }

    @Override
    protected void deallocate() {
        //nothing to do
    }

    @Override
    @Nonnull
    public ReferenceCounted touch(Object hint) {
        return this;
    }

    private class PortionView implements CharSequence {

        private int start;
        private int end;

        public PortionView(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public int length() {
            return end - start;
        }

        @Override
        public char charAt(int index) {
            return state[start + index];
        }

        @Override
        public CharSequence subSequence(int subSequenceStart, int subSequenceEnd) {
            return new PortionView(this.start + subSequenceStart, this.end - subSequenceEnd);
        }
    }
}
