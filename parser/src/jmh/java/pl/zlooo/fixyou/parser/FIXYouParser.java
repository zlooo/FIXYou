package pl.zlooo.fixyou.parser;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.openjdk.jmh.annotations.*;
import pl.zlooo.fixyou.parser.model.FixMessage;

@State(Scope.Benchmark)
public class FIXYouParser {
    //TODO add more fix messages to parse
    private static final String SAMPLE_FIX_MESSAGE = "8=FIX.4.2|9=154|35=6|49=BRKR|56=INVMGR|34=238|52=19980604-07:59:56|23=115686|28=N|55=FIA.MI|54=2|27=250000|44=7900.000000|25=H|10=231|";

    private ByteBuf byteBufBytes;
    private FixMessageReader fixMessageReader;

    @Setup
    public void setUp() {
        final byte[] msgBytes = SAMPLE_FIX_MESSAGE.replace('|', '\u0001').getBytes();
        byteBufBytes = Unpooled.directBuffer(msgBytes.length);
        byteBufBytes.writeBytes(msgBytes);
        fixMessageReader = new FixMessageReader();
    }

    @TearDown
    public void tearDown() {
        final FixMessage fixMessage = fixMessageReader.getFixMessage();
        if (fixMessage != null) {
            fixMessage.release();
        }
        byteBufBytes.release();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.SampleTime})
    public void fixMessageReaderTest(FIXYouParser parser) throws Exception {
        fixMessageReader.setFixBytes(parser.byteBufBytes);
        fixMessageReader.parseFixMsgBytes();
    }
}
