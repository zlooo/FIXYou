package io.github.zlooo.fixyou.fix.commons.config.validator

import org.assertj.core.api.Assertions
import spock.lang.Specification

class InitiatorConfigValidatorTest extends Specification {

    private InitiatorConfigValidator configValidator = new InitiatorConfigValidator()

    def "should validate FIXYou configuration"() {
        expect:
        Assertions.assertThat(configValidator.validateConfig(config)).containsOnly(errorMessages.<String> toArray([] as String[]))

        where:
        config                                                                                                                        | errorMessages
        io.github.zlooo.fixyou.FIXYouConfiguration.builder().build()                                                                                         | []
        io.github.zlooo.fixyou.FIXYouConfiguration.builder().reconnectIntervalMillis(1).build()                                                              | []
        io.github.zlooo.fixyou.FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface("wrongValue").reconnectIntervalMillis(-1).build() | [Messages.noReconnectInterval()]
    }

    def "should validate session config"() {
        expect:
        Assertions.assertThat(configValidator.validateSessionConfig(config)).containsOnly(errorMessages.<String> toArray([] as String[]))

        where:
        config                                                                                                                         | errorMessages
        new io.github.zlooo.fixyou.session.SessionConfig()                                                                                                                                   | [Messages.invalidPort(0), Messages.noHost()]
        new io.github.zlooo.fixyou.session.SessionConfig().setPort(666).setHost("wrong")                                                                                                     | [Messages.invalidHost()]
        new io.github.zlooo.fixyou.session.SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(false)                                                                             | []
        new io.github.zlooo.fixyou.session.SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(false).setEncryptMethod(io.github.zlooo.fixyou.FixConstants.ENCRYPTION_METHOD_DES) | [Messages.encryptionNotSupported()]
        new io.github.zlooo.fixyou.session.SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(false).setHeartbeatInterval(-19)                                                   | [Messages.positive(io.
                github.
                zlooo.
                fixyou.
                session.
                SessionConfig.Fields.heartbeatInterval)]
        new io.github.zlooo.fixyou.session.SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(false).setMessageStore(Mock(io.github.zlooo.fixyou.session.MessageStore))          | []
        new io.github.zlooo.fixyou.session.SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(true).setMessageStore(Mock(io.github.zlooo.fixyou.session.MessageStore))           | []
        new io.github.zlooo.fixyou.session.SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(true)                                                                              | [Messages.noPersistence()]

    }
}
