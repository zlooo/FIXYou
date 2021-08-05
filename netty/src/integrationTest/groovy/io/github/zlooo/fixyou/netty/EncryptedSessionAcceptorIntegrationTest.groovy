package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.FIXYouConfiguration
import org.assertj.core.api.Assertions
import org.springframework.core.io.ClassPathResource
import spock.lang.Timeout

@Timeout(30)
class EncryptedSessionAcceptorIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    @Override
    protected void customizeFixYOUConfig(FIXYouConfiguration.FIXYouConfigurationBuilder builder) {
        builder.sslEnabled(true).sslConfiguration(FIXYouConfiguration.SSLConfiguration.builder().certChainFilePath(new ClassPathResource("acceptorCertChain.pem").getFile().getAbsolutePath()).privateKeyFilePath(
                new ClassPathResource("acceptorKey.pem").getFile().getAbsolutePath()).build())
    }

    @Override
    protected String quickfixConfigFilePath() {
        return "quickfixConfigSSLInitiator.properties"
    }

    def "should establish encrypted session"() {
        when:
        initiator.start()
        pollingConditions.eventually {
            !testQuickfixApplication.loggedOnSessions.isEmpty()
        }

        then:
        Assertions.assertThat(testQuickfixApplication.loggedOnSessions).containsOnly(sessionID)
        sessionSateListener.loggedOn
        sessionSateListener.sessionState.connected.get()
        sessionSateListener.sessionState.logonSent
        !sessionSateListener.sessionState.logoutSent
        sessionSateListener.sessionState.channel != null
    }
}
