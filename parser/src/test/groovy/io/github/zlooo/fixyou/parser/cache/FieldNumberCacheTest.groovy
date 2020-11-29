package io.github.zlooo.fixyou.parser.cache

import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor

class FieldNumberCacheTest extends Specification {

    private FieldNumberCache fieldNumberCache = new FieldNumberCache()
    private Executor executor = Mock()

    def "should get value from cache"() {
        setup:
        FieldNumberCache.ByteBufWithSum byteBufWithSum = new FieldNumberCache.ByteBufWithSum(null, -1)
        fieldNumberCache.@cache.put(1, byteBufWithSum)

        when:
        def result = fieldNumberCache.getEncodedFieldNumber(1, executor)

        then:
        result.is(byteBufWithSum)
        0 * _
        fieldNumberCache.@executorRef.get() == null
    }

    def "should encode new value and add it to cache"() {
        when:
        def result = fieldNumberCache.getEncodedFieldNumber(1, executor)

        then:
        result != null
        result.getByteBuf().writerIndex() == 2
        result.getByteBuf().toString(0, 2, StandardCharsets.US_ASCII) == "1="
        result.getSumOfBytes() == 49 + 61
        fieldNumberCache.@executorRef.get().is(executor)
        fieldNumberCache.@cache.get(1).is(result)
        1 * executor.execute(_ as Runnable)>> { Runnable cmd ->
            cmd.run()
        }
        0*_
    }

    def "should encode new value and add it to cache but release previous one should it be put in the meantime by other session"() {
        setup:
        FieldNumberCache.ByteBufWithSum byteBufWithSum = new FieldNumberCache.ByteBufWithSum(Unpooled.buffer(), 1)

        when:
        def result = fieldNumberCache.getEncodedFieldNumber(1, executor)

        then:
        result != null
        result.getByteBuf().writerIndex() == 2
        result.getByteBuf().toString(0, 2, StandardCharsets.US_ASCII) == "1="
        result.getSumOfBytes() == 49 + 61
        fieldNumberCache.@executorRef.get().is(executor)
        fieldNumberCache.@cache.get(1).is(result)
        1 * executor.execute(_ as Runnable)>> { Runnable cmd ->
            fieldNumberCache.@cache.put(1, byteBufWithSum)
            cmd.run()
        }
        byteBufWithSum.byteBuf.refCnt()==0
        0*_
    }
}
