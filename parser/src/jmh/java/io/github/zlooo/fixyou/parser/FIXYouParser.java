package io.github.zlooo.fixyou.parser;

import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.parser.cache.FieldNumberCache;
import io.github.zlooo.fixyou.parser.model.FieldCodec;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
public class FIXYouParser {
    //TODO add more fix messages to parse
    private static final String SAMPLE_FIX_MESSAGE = "8=FIX.4.2|9=154|35=6|49=BRKR|56=INVMGR|34=238|52=19980604-07:59:56|23=115686|28=N|55=FIA.MI|54=2|27=250000|44=7900.000000|25=H|10=231|";

    private FixMessageParser fixMessageParser;

    @Setup
    public void setUp() {
        final byte[] msgBytes = SAMPLE_FIX_MESSAGE.replace('|', '\u0001').getBytes();
        final ByteBufComposer byteBufComposer = new ByteBufComposer(1);
        final ByteBuf byteBufBytes = Unpooled.directBuffer(msgBytes.length);
        byteBufBytes.writeBytes(msgBytes);
        byteBufComposer.addByteBuf(byteBufBytes);
        fixMessageParser = new FixMessageParser(byteBufComposer, new FixSpec50SP2(), new FixMessage(new FieldCodec(new FieldNumberCache())));
    }

    @TearDown
    public void tearDown() {
        final FixMessage fixMessage = fixMessageParser.getFixMessage();
        if (fixMessage != null) {
            fixMessage.release();
        }
        fixMessageParser.getBytesToParse().releaseData(0, Integer.MAX_VALUE);
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.SampleTime})
    public void fixMessageReaderTest(FIXYouParser state) throws Exception {
        state.fixMessageParser.parseFixMsgBytes();
    }
}
