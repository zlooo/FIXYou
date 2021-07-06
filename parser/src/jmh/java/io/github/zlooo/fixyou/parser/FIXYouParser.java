package io.github.zlooo.fixyou.parser;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.commons.memory.RegionPool;
import io.github.zlooo.fixyou.parser.model.OffHeapFixMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
public class FIXYouParser {
    //TODO add more fix messages to parse
    private static final String SAMPLE_FIX_MESSAGE = "8=FIX.4.2|9=154|35=6|49=BRKR|56=INVMGR|34=238|52=19980604-07:59:56|23=115686|28=N|55=FIA.MI|54=2|27=250000|44=7900.000000|25=H|10=231|";
    private static final int REGION_POOL_SIZE = 16;

    private FixMessageParser fixMessageParser;
    private RegionPool regionPool;

    @Setup
    public void setUp() {
        final byte[] msgBytes = SAMPLE_FIX_MESSAGE.replace('|', '\u0001').getBytes();
        final ByteBufComposer byteBufComposer = new ByteBufComposer(1);
        final ByteBuf byteBufBytes = Unpooled.directBuffer(msgBytes.length);
        byteBufBytes.writeBytes(msgBytes);
        byteBufComposer.addByteBuf(byteBufBytes);
        regionPool = new RegionPool(REGION_POOL_SIZE, DefaultConfiguration.REGION_SIZE);
        fixMessageParser = new FixMessageParser(byteBufComposer, new FixSpec50SP2(), new OffHeapFixMessage(regionPool));
    }

    @TearDown
    public void tearDown() {
        final OffHeapFixMessage fixMessage = (OffHeapFixMessage) fixMessageParser.getFixMessage();
        if (fixMessage != null) {
            fixMessage.close();
        }
        fixMessageParser.getBytesToParse().releaseData(0, Integer.MAX_VALUE);
        if (regionPool != null) {
            regionPool.close();
        }
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput, Mode.SampleTime})
    public void fixMessageReaderTest(FIXYouParser state) throws Exception {
        state.fixMessageParser.parseFixMsgBytes();
    }
}
