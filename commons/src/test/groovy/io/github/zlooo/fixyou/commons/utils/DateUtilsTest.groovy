package io.github.zlooo.fixyou.commons.utils

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.time.Instant

class DateUtilsTest extends Specification {

    @Unroll
    def "should write timestamp to buffer"() {
        setup:
        ByteBuf buf = Unpooled.buffer(21)

        when:
        DateUtils.writeTimestamp(timestamp.toEpochMilli(), buf, withMillis)

        then:
        buf.toString(StandardCharsets.US_ASCII) == expected

        where:
        timestamp                                 | expected            | withMillis
        Instant.parse("2020-12-03T10:15:30.000Z") | "20201203-10:15:30" | false
        Instant.parse("2020-12-03T10:15:30.000Z") | "20201203-10:15:30.000" | true
        Instant.parse("2020-02-01T00:00:00.000Z") | "20200201-00:00:00" | false
        Instant.parse("2020-02-01T00:00:00.000Z") | "20200201-00:00:00.000" | true
        Instant.parse("2020-03-01T00:00:00.123Z") | "20200301-00:00:00" | false
        Instant.parse("2020-03-01T00:00:00.123Z") | "20200301-00:00:00.123" | true
        Instant.parse("2020-01-03T10:15:30.456Z") | "20200103-10:15:30" | false
        Instant.parse("2020-01-03T10:15:30.456Z") | "20200103-10:15:30.456" | true
        Instant.parse("2021-01-03T10:15:30.456Z") | "20210103-10:15:30" | false
        Instant.parse("2021-01-03T10:15:30.456Z") | "20210103-10:15:30.456" | true
        Instant.parse("2021-12-03T10:15:30.456Z") | "20211203-10:15:30" | false
        Instant.parse("2021-12-03T10:15:30.456Z") | "20211203-10:15:30.456" | true
        Instant.parse("2022-12-03T10:15:30.999Z") | "20221203-10:15:30" | false
        Instant.parse("2022-12-03T10:15:30.999Z") | "20221203-10:15:30.999" | true
    }
}
