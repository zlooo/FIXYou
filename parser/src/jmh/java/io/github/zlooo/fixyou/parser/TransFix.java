package io.github.zlooo.fixyou.parser;

import net.openhft.fix.include.util.FixMessagePool;
import net.openhft.fix.include.v42.FixMessage;
import net.openhft.fix.include.v42.FixMessageReader;
import net.openhft.lang.io.ByteBufferBytes;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@State(Scope.Benchmark)
public class TransFix {

    public static final String SAMPLE_FIX_MESSAGE = "8=FIX.4.2|9=154|35=6|49=BRKR|56=INVMGR|34=238|52=19980604-07:59:56|23=115686|28=N|55=FIA.MI|54=2|27=250000|44=7900" +
            ".000000|25=H|10=231|";

    private FixMessagePool fmp;
    private ByteBufferBytes byteBufBytes;

    @Setup
    public void setUp() {
        final byte[] msgBytes = SAMPLE_FIX_MESSAGE.replace('|', '\u0001').getBytes();
        byteBufBytes = new ByteBufferBytes(ByteBuffer.allocate(msgBytes.length).order(ByteOrder.nativeOrder()));
        byteBufBytes.write(msgBytes);
        fmp = new FixMessagePool(null, Runtime.getRuntime().availableProcessors(), true);
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.SampleTime})
    public void transFix() throws Exception {
        final FixMessagePool.FixMessageContainer fmc = fmp.getFixMessageContainer();
        final FixMessage fm = fmc.getFixMessage();
        final FixMessageReader fmr = new FixMessageReader(fm);

        ByteBufferBytes reverseLookupBuf = null;
        final int[] fixFieldIds = {8, 9, 35, 49, 56, 34, 52, 23, 28, 55, 27, 44, 25, 10};
        fmr.setFixMessage(fm);
        fmr.setFixBytes(byteBufBytes);
        fmr.parseFixMsgBytes();
        byte[] readBytes = null;
        int len = -1;
        for (int j = 0; j < fixFieldIds.length; j++) {
            //reading data from ByteBufferByte back for verification
            reverseLookupBuf = fm.getField(fixFieldIds[j]).getFieldData();
            reverseLookupBuf.flip();
            len = (int) (reverseLookupBuf.limit() - reverseLookupBuf.position());
            readBytes = new byte[len];
            reverseLookupBuf.readFully(readBytes);
        }
    }
}
