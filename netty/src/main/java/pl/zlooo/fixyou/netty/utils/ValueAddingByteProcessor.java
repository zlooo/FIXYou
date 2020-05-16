package pl.zlooo.fixyou.netty.utils;

import io.netty.util.ByteProcessor;
import pl.zlooo.fixyou.Resettable;

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
