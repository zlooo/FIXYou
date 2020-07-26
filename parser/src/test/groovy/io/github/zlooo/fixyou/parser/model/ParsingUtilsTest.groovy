package io.github.zlooo.fixyou.parser.model

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
        def tempBuf = new byte[4]
        def chars = new char[4]

        when:
        ParsingUtils.readChars(byteBuff, 0, 4, tempBuf, chars)

        then:
        tempBuf == [116, 101, 115, 116] as byte[]
        chars == ['t', 'e', 's', 't'] as char[]
        byteBuff.readerIndex() == 4
        byteBuff.writerIndex() == 9
    }
}
