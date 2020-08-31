package io.github.zlooo.fixyou.parser.model

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class ParsingUtilsTest extends Specification {

    public static final int CAPACITY = 100

    def "should parse text from buffer"() {
        setup:
        def byteBuff = Unpooled.buffer(CAPACITY, CAPACITY)
        byteBuff.writeCharSequence("test", StandardCharsets.US_ASCII)
        byteBuff.writeCharSequence("test2", StandardCharsets.US_ASCII)
        byteBuff.readerIndex(4)
        def composer = new ByteBufComposer(1)
        composer.addByteBuf(byteBuff)
        def tempBuf = new byte[4]
        def chars = new char[4]

        when:
        ParsingUtils.readChars(composer, 0, 4, tempBuf, chars)

        then:
        tempBuf == [116, 101, 115, 116] as byte[]
        chars == ['t', 'e', 's', 't'] as char[]
        byteBuff.readerIndex() == 4
        byteBuff.writerIndex() == 9
    }

    def "should parse integer"() {
        expect:
        ParsingUtils.parseInteger(source, srcIndex, endIndicator, false) == result

        where:
        source                     | srcIndex | endIndicator || result
        comp("-123|")              | 0        | 124 as byte  || -123
        comp("garbage123garbage|") | 0        | 124 as byte  || 123
    }

    def "should parse long"() {
        expect:
        ParsingUtils.parseLong(source, srcIndex, endIndicator) == result

        where:
        source                     | srcIndex | endIndicator || result
        comp("-123|")              | 0        | 124 as byte  || -123L
        comp("garbage123garbage|") | 0        | 124 as byte  || 123L
    }

    private static ByteBufComposer comp(String value) {
        ByteBufComposer composer = new ByteBufComposer(1)
        composer.addByteBuf(Unpooled.wrappedBuffer(value.getBytes(StandardCharsets.US_ASCII)))
        return composer
    }
}
