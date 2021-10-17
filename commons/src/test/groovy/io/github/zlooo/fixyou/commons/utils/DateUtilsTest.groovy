package io.github.zlooo.fixyou.commons.utils


import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetTime

class DateUtilsTest extends Specification {

    def "should write timestamp to buffer"() {
        setup:
        ByteBuf buf = Unpooled.buffer(21)

        when:
        DateUtils.writeTimestamp(timestamp.toEpochMilli(), buf, withMillis)

        then:
        buf.toString(StandardCharsets.US_ASCII) == expected

        where:
        timestamp                                 | expected                | withMillis
        Instant.parse("2021-05-30T10:15:30.000Z") | "20210530-10:15:30"     | false
        Instant.parse("2021-05-30T10:15:30.000Z") | "20210530-10:15:30.000" | true
        Instant.parse("2021-12-03T10:15:30.000Z") | "20211203-10:15:30"     | false
        Instant.parse("2021-12-03T10:15:30.000Z") | "20211203-10:15:30.000" | true
        Instant.parse("2021-02-01T00:00:00.000Z") | "20210201-00:00:00"     | false
        Instant.parse("2021-02-01T00:00:00.000Z") | "20210201-00:00:00.000" | true
        Instant.parse("2021-03-01T00:00:00.123Z") | "20210301-00:00:00"     | false
        Instant.parse("2021-03-01T00:00:00.123Z") | "20210301-00:00:00.123" | true
        Instant.parse("2021-01-03T10:15:30.456Z") | "20210103-10:15:30"     | false
        Instant.parse("2021-01-03T10:15:30.456Z") | "20210103-10:15:30.456" | true
        Instant.parse("2022-01-03T10:15:30.456Z") | "20220103-10:15:30"     | false
        Instant.parse("2022-01-03T10:15:30.456Z") | "20220103-10:15:30.456" | true
        Instant.parse("2022-12-03T10:15:30.456Z") | "20221203-10:15:30"     | false
        Instant.parse("2022-12-03T10:15:30.456Z") | "20221203-10:15:30.456" | true
        Instant.parse("2023-12-03T10:15:30.999Z") | "20231203-10:15:30"     | false
        Instant.parse("2023-12-03T10:15:30.999Z") | "20231203-10:15:30.999" | true
        Instant.parse("2021-08-31T05:59:20.808Z") | "20210831-05:59:20.808" | true
        Instant.parse("2024-08-31T05:59:20.808Z") | "20240831-05:59:20.808" | true
    }

    def "should parse timestamp"() {
        setup:
        def buffer = Unpooled.wrappedBuffer(timestamp.getBytes(StandardCharsets.US_ASCII))

        expect:
        DateUtils.parseTimestamp(buffer, new DateUtils.TimestampParser()) == expectedResult

        where:
        timestamp               | expectedResult
        "20210530-10:15:30"     | Instant.parse("2021-05-30T10:15:30.000Z").toEpochMilli()
        "20210530-10:15:30.000" | Instant.parse("2021-05-30T10:15:30.000Z").toEpochMilli()
        "20211203-10:15:30"     | Instant.parse("2021-12-03T10:15:30.000Z").toEpochMilli()
        "20211203-10:15:30.000" | Instant.parse("2021-12-03T10:15:30.000Z").toEpochMilli()
        "20210201-00:00:00"     | Instant.parse("2021-02-01T00:00:00.000Z").toEpochMilli()
        "20210201-00:00:00.000" | Instant.parse("2021-02-01T00:00:00.000Z").toEpochMilli()
        "20210301-00:00:00"     | Instant.parse("2021-03-01T00:00:00.000Z").toEpochMilli()
        "20210301-00:00:00.123" | Instant.parse("2021-03-01T00:00:00.123Z").toEpochMilli()
        "20210103-10:15:30"     | Instant.parse("2021-01-03T10:15:30.000Z").toEpochMilli()
        "20210103-10:15:30.456" | Instant.parse("2021-01-03T10:15:30.456Z").toEpochMilli()
        "20220103-10:15:30"     | Instant.parse("2022-01-03T10:15:30.000Z").toEpochMilli()
        "20220103-10:15:30.456" | Instant.parse("2022-01-03T10:15:30.456Z").toEpochMilli()
        "20221203-10:15:30"     | Instant.parse("2022-12-03T10:15:30.000Z").toEpochMilli()
        "20221203-10:15:30.456" | Instant.parse("2022-12-03T10:15:30.456Z").toEpochMilli()
        "20231203-10:15:30"     | Instant.parse("2023-12-03T10:15:30.000Z").toEpochMilli()
        "20231203-10:15:30.999" | Instant.parse("2023-12-03T10:15:30.999Z").toEpochMilli()
        "20210831-05:59:20.808" | Instant.parse("2021-08-31T05:59:20.808Z").toEpochMilli()
        "20240831-05:59:20.808" | Instant.parse("2024-08-31T05:59:20.808Z").toEpochMilli()
    }

    def "should reset timestamp parser"() {
        setup:
        def timestampParser = new DateUtils.TimestampParser()
        def freshParser = new DateUtils.TimestampParser()
        def buffer = Unpooled.wrappedBuffer("20210530-10:15:30".getBytes(StandardCharsets.US_ASCII))
        DateUtils.parseTimestamp(buffer, timestampParser)

        when:
        timestampParser.reset()

        then:
        timestampParser.@result == freshParser.@result
        timestampParser.@temp == freshParser.@temp
        timestampParser.@counter == freshParser.@counter
        timestampParser.@isLeapYear == freshParser.@isLeapYear
    }

    def "should not parse wrong timestamp"() {
        setup:
        def buffer = Unpooled.wrappedBuffer(timestamp.getBytes(StandardCharsets.US_ASCII))

        when:
        DateUtils.parseTimestamp(buffer, new DateUtils.TimestampParser())

        then:
        thrown(exception)

        where:
        timestamp                  | exception
        "20210831-05:59:20.808123" | IndexOutOfBoundsException
        "20213031-05:59:20.808"    | IllegalArgumentException
    }

    def "should convert to epoch millis"() {
        setup:
        //Sunday
        def instant = LocalDate.parse("2021-10-10")

        expect:
        DateUtils.epochMillis(instant, dayOfWeek, offsetTime, previousDay) == expected.toEpochMilli()

        where:
        dayOfWeek         | offsetTime                             | previousDay || expected
        null              | OffsetTime.parse("17:34:24.789+01:00") | false       || Instant.parse("2021-10-10T16:34:24.789Z")
        DayOfWeek.SUNDAY  | OffsetTime.parse("17:34:24.789+01:00") | false       || Instant.parse("2021-10-10T16:34:24.789Z")
        null              | OffsetTime.parse("03:34:24.789+01:00") | false       || Instant.parse("2021-10-10T02:34:24.789Z")
        DayOfWeek.SUNDAY  | OffsetTime.parse("03:34:24.789+01:00") | false       || Instant.parse("2021-10-10T02:34:24.789Z")
        DayOfWeek.TUESDAY | OffsetTime.parse("03:34:24.789+01:00") | false       || Instant.parse("2021-10-12T02:34:24.789Z")

        null              | OffsetTime.parse("17:34:24.789+01:00") | true        || Instant.parse("2021-10-10T16:34:24.789Z")
        DayOfWeek.SUNDAY  | OffsetTime.parse("17:34:24.789+01:00") | true        || Instant.parse("2021-10-10T16:34:24.789Z")
        null              | OffsetTime.parse("03:34:24.789+01:00") | true        || Instant.parse("2021-10-10T02:34:24.789Z")
        DayOfWeek.SUNDAY  | OffsetTime.parse("03:34:24.789+01:00") | true        || Instant.parse("2021-10-10T02:34:24.789Z")
        DayOfWeek.TUESDAY | OffsetTime.parse("03:34:24.789+01:00") | true        || Instant.parse("2021-10-05T02:34:24.789Z")
    }
}
