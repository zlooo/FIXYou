package io.github.zlooo.fixyou.fix.commons.config.validator

import io.github.zlooo.fixyou.FIXYouConfiguration
import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.session.MessageStore
import io.github.zlooo.fixyou.session.SessionConfig
import org.assertj.core.api.Assertions
import spock.lang.Specification

class AcceptorConfigValidatorTest extends Specification {

    private AcceptorConfigValidator configValidator = new AcceptorConfigValidator()

    def "should validate FIXYou configuration"() {
        expect:
        Assertions.assertThat(configValidator.validateConfig(config)).containsOnly(errorMessages.<String> toArray([] as String[]))

        where:
        config                                                                                                                                                                                               | errorMessages
        FIXYouConfiguration.builder().acceptorListenPort(666).build()                                                                                                                                        | []
        FIXYouConfiguration.builder().acceptorListenPort(666666).build()                                                                                                                                     | [Messages.invalidPort(666666)]
        FIXYouConfiguration.builder().acceptorListenPort(-10).build()                                                                                                                                        | [Messages.invalidPort(-10)]
        FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface(null).build()                                                                                                            |
        [Messages.invalidPort(-10), Messages.noBindInterface()]
        FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface("").build()                                                                                                              |
        [Messages.invalidPort(-10), Messages.noBindInterface()]
        FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface("wrongValue").build()                                                                                                    |
        [Messages.invalidPort(-10), Messages.invalidBindInterface()]
        FIXYouConfiguration.builder().acceptorListenPort(1234).acceptorBindInterface("wrongValue").build()                                                                                                   | [Messages.invalidBindInterface()]
        FIXYouConfiguration.builder().acceptorListenPort(666).sslEnabled(true).build()                                                                                                                       | [Messages.noSslConfig()]
        FIXYouConfiguration.builder().acceptorListenPort(666).sslEnabled(true).sslConfiguration(FIXYouConfiguration.SSLConfiguration.builder().build()).build()                                              |
        [Messages.noCertChainFile(), Messages.noPrivateKeyFile()]
        FIXYouConfiguration.builder().acceptorListenPort(666).sslEnabled(true).sslConfiguration(FIXYouConfiguration.SSLConfiguration.builder().certChainFilePath("").privateKeyFilePath("").build()).build() |
        [Messages.noCertChainFile(), Messages.noPrivateKeyFile()]
    }

    def "should validate session config"() {
        expect:
        Assertions.assertThat(configValidator.validateSessionConfig(config)).containsOnly(errorMessages.<String> toArray([] as String[]))

        where:
        config                                                                             | errorMessages
        SessionConfig.builder().build()                                                    | []
        SessionConfig.builder().encryptMethod(FixConstants.ENCRYPTION_METHOD_DES).build()  | [Messages.encryptionNotSupported()]
        SessionConfig.builder().heartbeatInterval(-666).build()                            | [Messages.positive(SessionConfig.Fields.heartbeatInterval)]
        SessionConfig.builder().persistent(false).build()                                  | []
        SessionConfig.builder().persistent(false).messageStore(Mock(MessageStore)).build() | []
        SessionConfig.builder().persistent(true).messageStore(Mock(MessageStore)).build()  | []
        SessionConfig.builder().persistent(true).build()                                   | [Messages.noPersistence()]

    }
}
