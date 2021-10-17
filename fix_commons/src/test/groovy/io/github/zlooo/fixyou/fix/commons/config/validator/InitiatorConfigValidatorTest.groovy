package io.github.zlooo.fixyou.fix.commons.config.validator

import io.github.zlooo.fixyou.FIXYouConfiguration
import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.session.MessageStore
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.StartStopConfig
import org.assertj.core.api.Assertions
import spock.lang.Specification

import java.time.DayOfWeek
import java.time.OffsetTime

class InitiatorConfigValidatorTest extends Specification {

    private InitiatorConfigValidator configValidator = new InitiatorConfigValidator()

    def "should validate FIXYou configuration"() {
        expect:
        Assertions.assertThat(configValidator.validateConfig(config)).containsOnly(errorMessages.<String> toArray([] as String[]))

        where:
        config                                                                                                                        | errorMessages
        FIXYouConfiguration.builder().build()                                                                                         | []
        FIXYouConfiguration.builder().reconnectIntervalMillis(1).build()                                                              | []
        FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface("wrongValue").reconnectIntervalMillis(-1).build() | [Messages.noReconnectInterval()]
    }

    def "should validate session config"() {
        expect:
        Assertions.assertThat(configValidator.validateSessionConfig(config)).containsOnly(errorMessages.<String> toArray([] as String[]))

        where:
        config                                                                                                                                                                                                            | errorMessages
        SessionConfig.builder().build()                                                                                                                                                                                   |
        [Messages.invalidPort(0), Messages.noHost()]
        SessionConfig.builder().port(666).host("wrong").build()                                                                                                                                                           |
        [Messages.invalidHost()]
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(false).build()                                                                                                                                      | []
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(false).encryptMethod(FixConstants.ENCRYPTION_METHOD_DES).build()                                                                                    |
        [Messages.encryptionNotSupported()]
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(false).heartbeatInterval(-19).build()                                                                                                               |
        [Messages.positive(SessionConfig.Fields.heartbeatInterval)]
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(false).messageStore(Mock(MessageStore)).build()                                                                                                     | []
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(true).messageStore(Mock(MessageStore)).build()                                                                                                      | []
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(true).build()                                                                                                                                       |
        [Messages.noPersistence()]
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(false).startStopConfig(StartStopConfig.INFINITE).build()                                                                                            | []
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(false).startStopConfig(StartStopConfig.builder().build()).build()                                                                                   |
        [Messages.startStopTimes()]
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(false).startStopConfig(StartStopConfig.builder().startDay(DayOfWeek.MONDAY).build()).build()                                                        |
        [Messages.startStopTimes(), Messages.startStopDays()]
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(false).startStopConfig(StartStopConfig.builder().startDay(DayOfWeek.MONDAY).stopDay(DayOfWeek.TUESDAY).build()).build()                             |
        [Messages.startStopTimes(), Messages.startStopDaysNoTime()]
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(false).startStopConfig(StartStopConfig.builder().startTime(OffsetTime.now()).stopTime(OffsetTime.now()).build()).build()                            | []
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(false).startStopConfig(StartStopConfig.builder().startTime(OffsetTime.now()).stopTime(OffsetTime.now()).startDay(DayOfWeek.MONDAY).build()).build() |
        [Messages.startStopDays()]
        SessionConfig.builder().port(666).host("10.0.0.1").persistent(false)
                     .startStopConfig(StartStopConfig.builder().startTime(OffsetTime.now()).stopTime(OffsetTime.now()).startDay(DayOfWeek.MONDAY).stopDay(DayOfWeek.MONDAY).build()).build()                              | []
    }
}
