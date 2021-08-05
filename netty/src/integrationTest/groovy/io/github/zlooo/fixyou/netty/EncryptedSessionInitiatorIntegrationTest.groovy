package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.FIXYouConfiguration
import org.springframework.core.io.ClassPathResource
import spock.lang.Timeout

@Timeout(30)
class EncryptedSessionInitiatorIntegrationTest extends AbstractFixYOUInitiatorIntegrationTest {

    @Override
    protected String quickfixConfigFilePath() {
        return "quickfixConfigSSLAcceptor.properties"
    }

    @Override
    protected void customizeFixYOUConfig(FIXYouConfiguration.FIXYouConfigurationBuilder builder) {
        builder.sslEnabled(true).sslConfiguration(FIXYouConfiguration.SSLConfiguration.builder().trustChainFilePath(new ClassPathResource ("acceptorCertChain.pem").getFile().getAbsolutePath()).build())
    }

    def "should establish encrypted session"(){
        setup:
        acceptor.start()

        when:
        engine.start()
        while (!sessionSateListener.loggedOn) {
            Thread.sleep(500)
        }

        then:
        sessionSateListener.loggedOn
        sessionSateListener.sessionState.connected.get()
        sessionSateListener.sessionState.logonSent
        !sessionSateListener.sessionState.logoutSent
        sessionSateListener.sessionState.channel != null
    }
}
