package io.github.zlooo.fixyou.netty.utils;

import io.github.zlooo.fixyou.Resettable;
import io.netty.util.ByteProcessor;

public class ValueAddingByteProcessor implements ByteProcessor, Resettable {

    private int result;

    @Override
    public boolean process(byte value) throws Exception {
        result += value;
        return true;
    }

    public int getResult() {
        return result;
    }

    @Override
    public void reset() {
        result = 0;
    }
}
