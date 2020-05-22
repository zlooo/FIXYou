package io.github.zlooo.fixyou.fix.commons.config.validator

import org.assertj.core.api.Assertions
import spock.lang.Specification

class AcceptorConfigValidatorTest extends Specification {

    private AcceptorConfigValidator configValidator = new AcceptorConfigValidator()

    def "should validate FIXYou configuration"() {
        expect:
        Assertions.assertThat(configValidator.validateConfig(config)).containsOnly(errorMessages.<String> toArray([] as String[]))

        where:
        config                                                                                             | errorMessages
        io.github.zlooo.fixyou.FIXYouConfiguration.builder().acceptorListenPort(666).build()                                      | []
        io.github.zlooo.fixyou.FIXYouConfiguration.builder().acceptorListenPort(666666).build()                                   | [Messages.invalidPort(666666)]
        io.github.zlooo.fixyou.FIXYouConfiguration.builder().acceptorListenPort(-10).build()                                      | [Messages.invalidPort(-10)]
        io.github.zlooo.fixyou.FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface(null).build()          | [Messages.invalidPort(-10), Messages.noBindInterface()]
        io.github.zlooo.fixyou.FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface("").build()            | [Messages.invalidPort(-10), Messages.noBindInterface()]
        io.github.zlooo.fixyou.FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface("wrongValue").build()  | [Messages.invalidPort(-10), Messages.invalidBindInterface()]
        io.github.zlooo.fixyou.FIXYouConfiguration.builder().acceptorListenPort(1234).acceptorBindInterface("wrongValue").build() | [Messages.invalidBindInterface()]
    }

    def "should validate session config"() {
        expect:
        Assertions.assertThat(configValidator.validateSessionConfig(config)).containsOnly(errorMessages.<String> toArray([] as String[]))

        where:
        config                                                                       | errorMessages
        new io.github.zlooo.fixyou.session.SessionConfig()                                                                                         | []
        new io.github.zlooo.fixyou.session.SessionConfig().setEncryptMethod(io.github.zlooo.fixyou.FixConstants.ENCRYPTION_METHOD_DES)             | [Messages.encryptionNotSupported()]
        new io.github.zlooo.fixyou.session.SessionConfig().setHeartbeatInterval(-666)                                                              | [Messages.positive(io.github.zlooo.fixyou.session.SessionConfig.Fields.heartbeatInterval)]
        new io.github.zlooo.fixyou.session.SessionConfig().setPersistent(false)                                                                    | []
        new io.github.zlooo.fixyou.session.SessionConfig().setPersistent(false).setMessageStore(Mock(io.github.zlooo.fixyou.session.MessageStore)) | []
        new io.github.zlooo.fixyou.session.SessionConfig().setPersistent(true).setMessageStore(Mock(io.github.zlooo.fixyou.session.MessageStore))  | []
        new io.github.zlooo.fixyou.session.SessionConfig().setPersistent(true)                                                                     | [Messages.noPersistence()]

    }
}
