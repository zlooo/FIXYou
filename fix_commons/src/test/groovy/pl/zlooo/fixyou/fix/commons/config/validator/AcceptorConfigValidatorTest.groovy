package pl.zlooo.fixyou.fix.commons.config.validator

import org.assertj.core.api.Assertions
import pl.zlooo.fixyou.FIXYouConfiguration
import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.session.MessageStore
import pl.zlooo.fixyou.session.SessionConfig
import spock.lang.Specification

class AcceptorConfigValidatorTest extends Specification {

    private AcceptorConfigValidator configValidator = new AcceptorConfigValidator()

    def "should validate FIXYou configuration"() {
        expect:
        Assertions.assertThat(configValidator.validateConfig(config)).containsOnly(errorMessages.<String> toArray([] as String[]))

        where:
        config                                                                                             | errorMessages
        FIXYouConfiguration.builder().acceptorListenPort(666).build()                                      | []
        FIXYouConfiguration.builder().acceptorListenPort(666666).build()                                   | [Messages.invalidPort(666666)]
        FIXYouConfiguration.builder().acceptorListenPort(-10).build()                                      | [Messages.invalidPort(-10)]
        FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface(null).build()          | [Messages.invalidPort(-10), Messages.noBindInterface()]
        FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface("").build()            | [Messages.invalidPort(-10), Messages.noBindInterface()]
        FIXYouConfiguration.builder().acceptorListenPort(-10).acceptorBindInterface("wrongValue").build()  | [Messages.invalidPort(-10), Messages.invalidBindInterface()]
        FIXYouConfiguration.builder().acceptorListenPort(1234).acceptorBindInterface("wrongValue").build() | [Messages.invalidBindInterface()]
    }

    def "should validate session config"() {
        expect:
        Assertions.assertThat(configValidator.validateSessionConfig(config)).containsOnly(errorMessages.<String> toArray([] as String[]))

        where:
        config                                                                       | errorMessages
        new SessionConfig()                                                          | []
        new SessionConfig().setEncryptMethod(FixConstants.ENCRYPTION_METHOD_DES)     | [Messages.encryptionNotSupported()]
        new SessionConfig().setHeartbeatInterval(-666)                               | [Messages.positive(SessionConfig.Fields.heartbeatInterval)]
        new SessionConfig().setPersistent(false)                                     | []
        new SessionConfig().setPersistent(false).setMessageStore(Mock(MessageStore)) | []
        new SessionConfig().setPersistent(true).setMessageStore(Mock(MessageStore))  | []
        new SessionConfig().setPersistent(true)                                      | [Messages.noPersistence()]

    }
}
