package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.Engine
import io.github.zlooo.fixyou.FIXYouConfiguration
import io.github.zlooo.fixyou.commons.memory.RegionPool
import io.github.zlooo.fixyou.netty.test.framework.*
import io.github.zlooo.fixyou.session.SessionConfig
import org.springframework.util.SocketUtils
import quickfix.Acceptor
import quickfix.SessionID
import spock.lang.Shared
import spock.lang.Specification

abstract class AbstractFixYOUInitiatorIntegrationTest extends Specification {

    protected static final String senderCompId = "testInitiator"
    protected static final String targetCompId = "testAcceptor"

    protected int port
    protected Engine engine
    protected Acceptor acceptor
    protected SessionID sessionID = new SessionID("FIXT.1.1", targetCompId, senderCompId)
    protected TestQuickfixApplication testQuickfixApplication = new TestQuickfixApplication()
    protected TestSessionSateListener sessionSateListener = new TestSessionSateListener()
    @Shared
    private final RegionPool regionPool = new RegionPool(50, 256 as short)

    void setup() {
        port = SocketUtils.findAvailableTcpPort()
        def configBuilder = FIXYouConfiguration.builder().initiator(true).fixMessagePoolSize(4).fixMessageListenerInvokerDisruptorSize(4)
        customizeFixYOUConfig(configBuilder)
        engine = FIXYouNetty.
                create(configBuilder.build(), new TestFixMessageListener(regionPool)).registerSession(new io.github.zlooo.fixyou.session.SessionID("FIXT.1.1", senderCompId, targetCompId,), TestSpec.INSTANCE,
                                                                                                      //TODO test spec for now but once we have real one it should be used here instead
                                                                                                      SessionConfig.builder().persistent(false).host("localhost").port(port).sessionStateListener(sessionSateListener).build())
        acceptor = QuickfixTestUtils.setupAcceptor(port, sessionID, testQuickfixApplication, quickfixConfigFilePath())
    }

    protected void customizeFixYOUConfig(FIXYouConfiguration.FIXYouConfigurationBuilder builder) {
        //nothing to do by default
    }

    protected String quickfixConfigFilePath() {
        return "quickfixConfigAcceptor.properties"
    }

    void cleanup() {
        engine?.stop()
        acceptor?.stop(true)
    }

    def cleanupSpec() {
        regionPool.close()
    }
}
