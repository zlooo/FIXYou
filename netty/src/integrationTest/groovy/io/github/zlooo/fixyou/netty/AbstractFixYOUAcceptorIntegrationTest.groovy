package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.FIXYouConfiguration
import io.github.zlooo.fixyou.Resettable
import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.commons.pool.AbstractPoolableObject
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.handler.NettyResettablesNames
import io.github.zlooo.fixyou.netty.test.framework.*
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.ValidationConfig
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import org.assertj.core.api.Assertions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.spockframework.util.Assert
import org.springframework.util.SocketUtils
import quickfix.Initiator
import quickfix.SessionID
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.charset.StandardCharsets

class AbstractFixYOUAcceptorIntegrationTest extends Specification {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFixYOUAcceptorIntegrationTest)

    protected static final String senderCompId = "testInitiator"
    protected static final String targetCompId = "testAcceptor"
    protected static final String qualifier = "secondSession"
    private static final char[] CHECKSUM_TAG_INDICATOR = ['1', '0', '='] as char[]
    protected SessionID sessionID = new SessionID("FIXT.1.1", targetCompId, senderCompId)
    protected fixYouSessionId = new io.github.zlooo.fixyou.session.SessionID("FIXT.1.1".toCharArray(), 8, senderCompId.toCharArray(), senderCompId.length(), targetCompId.toCharArray(), targetCompId.length())
    protected TestSessionSateListener sessionSateListener = new TestSessionSateListener()
    protected FIXYouNettyAcceptor engine
    protected Initiator initiator
    protected TestQuickfixApplication testQuickfixApplication = new TestQuickfixApplication()
    protected int acceptorPort
    protected TestFixMessageListener testFixMessageListener = new TestFixMessageListener()
    protected PollingConditions pollingConditions = new PollingConditions(timeout: 30)
    private EventLoopGroup group
    protected List<String> receivedMessages = Collections.synchronizedList(new ArrayList())

    void setup() {
        LOGGER.info("Setup for test {}", getSpecificationContext().getCurrentFeature().getName())
        acceptorPort = SocketUtils.findAvailableTcpPort()
        LOGGER.debug("Starting FIXYou, listening on port {}", acceptorPort)
        engine = FIXYouNetty.
                create(FIXYouConfiguration.builder().acceptorListenPort(acceptorPort).initiator(false).fixMessagePoolSize(4).fixMessageListenerInvokerDisruptorSize(8).build(), testFixMessageListener).
                //TODO test spec for now but once we have real one it should be used here instead
                        registerSession(fixYouSessionId, TestSpec.INSTANCE, createConfig())
        engine.start().get()
        LOGGER.debug("Creating quickfix initiator")
        initiator = QuickfixTestUtils.setupInitiator(acceptorPort, sessionID, testQuickfixApplication)
        LOGGER.info("Setup done")
    }

    protected SessionConfig createConfig() {
        def sessionConfig = new SessionConfig().setPersistent(false).addSessionStateListener(sessionSateListener).setValidationConfig(new ValidationConfig().setValidate(true).
                setShouldCheckSendingTime(true).
                setShouldCheckSessionIDAfterLogon(true).
                setShouldCheckBodyLength(true).
                setShouldCheckOrigVsSendingTime(true).
                setShouldCheckMessageType(true).
                setShouldCheckMessageChecksum(true))
        LOGGER.debug("Session config: {}", sessionConfig)
        return sessionConfig
    }

    protected Channel connect() {
        group = new NioEventLoopGroup(1)
        return new Bootstrap().channel(NioSocketChannel).group(group).handler(new ChannelInboundHandlerAdapter() {
            @Override
            void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof ByteBuf) {
                    receivedMessages.addAll(splitMessagesIfNecessary(msg.readCharSequence(msg.readableBytes(), StandardCharsets.US_ASCII)))
                }
                super.channelRead(ctx, msg)
            }

        }).connect("localhost", acceptorPort).sync().channel()
    }

    protected void waitForLogonResponse() {
        pollingConditions.eventually {
            !receivedMessages.isEmpty()
            receivedMessages.any { it.contains("35=A") }
        }
    }

    static List<String> splitMessagesIfNecessary(CharSequence message) {
        def result = []
        int messageStartIndex = 0
        int checksumIndicatorIndex = 0
        boolean checksumFound = false
        for (int i = 0; i < message.length(); i++) {
            if (!checksumFound) {
                if (message.charAt(i) == CHECKSUM_TAG_INDICATOR[checksumIndicatorIndex]) {
                    checksumIndicatorIndex++
                    if (checksumIndicatorIndex == CHECKSUM_TAG_INDICATOR.length) {
                        checksumFound = true
                    }
                } else {
                    checksumIndicatorIndex = 0
                }
            } else if (message.charAt(i) == 0x01 as char) {
                result << message.subSequence(messageStartIndex, i + 1).toString()
                messageStartIndex = i + 1
                checksumFound = false
                checksumIndicatorIndex = 0
            }
        }
        return result
    }

    protected ChannelFuture sendMessage(Channel channel, String message) {
        return channel.writeAndFlush(Unpooled.wrappedBuffer(message.replaceAll("\n", "").replaceAll(" ", "").getBytes(StandardCharsets.US_ASCII))).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).sync()
    }

    protected long nextExpectedInboundSequenceNumber() {
        return sessionHandler().@nextExpectedInboundSequenceNumber
    }

    protected Resettable sessionHandler() {
        return ((FIXYouNettyAcceptor) engine).@fixYouNettyComponent.sessionRegistry().getStateForSession(fixYouSessionId).resettables[
                NettyResettablesNames.SESSION]
    }

    protected Resettable messageDecoder() {
        return ((FIXYouNettyAcceptor) engine).@fixYouNettyComponent.sessionRegistry().getStateForSession(fixYouSessionId).resettables[
                NettyResettablesNames.MESSAGE_DECODER]
    }

    protected void nextExpectedInboundSequenceNumber(long nextExpectedInboundSequenceNumber) {
        sessionHandler().@nextExpectedInboundSequenceNumber = nextExpectedInboundSequenceNumber
    }

    protected void startQuickfixAndWaitTillLoggedIn() {
        LOGGER.debug("Starting quickfix")
        initiator.start()
        pollingConditions.eventually {
            !testQuickfixApplication.loggedOnSessions.isEmpty()
        }
        LOGGER.debug("Started and got a session logged in")
    }

    protected Collection<FixMessage> getInUseFixMessages() {
        def objectPool = engine.fixYouNettyComponent.fixMessageObjectPool()
        if (objectPool.@objectArray.contains(null)) {
            Assert.fail("Array in FixMessage object pool contains nulls, this means something has not been returned to the pool, which is baaaaaaaad")
        }
        def inUseMessages = objectPool.@objectArray.findAll { it.getState().get() == AbstractPoolableObject.IN_USE_STATE }
        if (objectPool instanceof DefaultObjectPool) {
            if (objectPool.@firstObject.getState()?.get() == AbstractPoolableObject.IN_USE_STATE) {
                inUseMessages.add(objectPool.@firstObject)
            }
            if (objectPool.@secondObject.getState()?.get() == AbstractPoolableObject.IN_USE_STATE) {
                inUseMessages.add(objectPool.@secondObject)
            }
        }
        return inUseMessages
    }

    void cleanup() {
        LOGGER.info("Cleanup after test {}", getSpecificationContext().getCurrentFeature().getName())
        def inUseFixMessages = getInUseFixMessages()
        if (!inUseFixMessages.isEmpty()) {
            Assert.fail("Not all FixMessages have been returned to the pool!!!! In use messages " + inUseFixMessages)
        }
        asssertComposerState(messageDecoder().@byteBufComposer)
        group?.shutdownGracefully()?.sync()
        engine?.stop()?.get()
        initiator?.stop(true)
        receivedMessages?.clear()
        LOGGER.info("Cleanup done")
    }

    void asssertComposerState(ByteBufComposer composer) {
        assert composer.storedStartIndex == ByteBufComposer.INITIAL_VALUE
        assert composer.storedEndIndex == ByteBufComposer.INITIAL_VALUE
        Assertions.assertThat(composer.components).containsOnly(new ByteBufComposer.Component())
    }
}
