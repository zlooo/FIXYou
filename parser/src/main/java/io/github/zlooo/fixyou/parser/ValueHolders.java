package io.github.zlooo.fixyou.parser;

import lombok.Data;

public class ValueHolders {

    @Data
    public static class IntHolder {
        private int value;
        private boolean negative;

        public IntHolder() {
        }

        public IntHolder(int value) {
            this.value = value;
        }

        public void reset() {
            value = 0;
            negative = false;
        }

        public int getAndIncrement() {
            return value++;
        }
    }

    @Data
    public static class LongHolder {
        private long value;
        private boolean negative;

        public void reset() {
            value = 0;
            negative = false;
        }

        public void increment(byte charSize) {
            value += charSize;
        }
    }

    @Data
    public static class DecimalHolder {
        private long unscaledValue;
        private short scale;

        public void reset() {
            unscaledValue = 0;
            scale = 0;
        }
    }
}
