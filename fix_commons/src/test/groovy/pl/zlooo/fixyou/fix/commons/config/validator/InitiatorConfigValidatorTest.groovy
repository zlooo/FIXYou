package pl.zlooo.fixyou.fix.commons.config.validator

import org.assertj.core.api.Assertions
import pl.zlooo.fixyou.FIXYouConfiguration
import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.session.MessageStore
import pl.zlooo.fixyou.session.SessionConfig
import spock.lang.Specification

class InitiatorConfigValidatorTest extends Specification {

    private InitiatorConfigValidator configValidator = new InitiatorConfigValidator()

    def "should validate FIXYou configuration"() {
        expect:
        Assertions.assertThat(configValidator.validateConfig(config)).containsOnly(errorMessages.<String> toArray({ size -> new String[size] }))

        where:
        config                                                                                                                        | errorMessages
        FIXYouConfiguration.builder().build()                                                                                         | []
        FIXYouConfiguration.builder().reconnectIntervalMillis(1).build()                                                              | []
        FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface("wrongValue").reconnectIntervalMillis(-1).build() | [Messages.noReconnectInterval()]
    }

    def "should validate session config"() {
        expect:
        Assertions.assertThat(configValidator.validateSessionConfig(config)).containsOnly(errorMessages.<String> toArray({ size -> new String[size] }))

        where:
        config                                                                                                                         | errorMessages
        new SessionConfig()                                                                                                            | [Messages.invalidPort(0), Messages.noHost()]
        new SessionConfig().setPort(666).setHost("wrong")                                                                              | [Messages.invalidHost()]
        new SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(false)                                                      | []
        new SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(false).setEncryptMethod(FixConstants.ENCRYPTION_METHOD_DES) | [Messages.encryptionNotSupported()]
        new SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(false).setHeartbeatInterval(-19)                            | [Messages.positive(SessionConfig.Fields.heartbeatInterval)]
        new SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(false).setMessageStore(Mock(MessageStore))                  | []
        new SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(true).setMessageStore(Mock(MessageStore))                   | []
        new SessionConfig().setPort(666).setHost("10.0.0.1").setPersistent(true)                                                       | [Messages.noPersistence()]

    }
}
