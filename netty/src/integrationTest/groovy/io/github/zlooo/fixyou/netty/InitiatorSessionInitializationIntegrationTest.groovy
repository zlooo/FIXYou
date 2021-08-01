package io.github.zlooo.fixyou.netty


import io.github.zlooo.fixyou.netty.test.framework.QuickfixTestUtils
import io.github.zlooo.fixyou.netty.test.framework.TestSessionSateListener
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import quickfix.Session
import quickfix.field.SessionRejectReason
import quickfix.fixt11.Logon
import quickfix.fixt11.Logout
import quickfix.fixt11.Reject
import spock.lang.Timeout

import java.nio.charset.StandardCharsets

@Timeout(30)
class InitiatorSessionInitializationIntegrationTest extends AbstractFixYOUInitiatorIntegrationTest {

    def "should initialize session 1B-c-0"() {
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

    def "should initialize session and send resend request 1B-c-1"() {
        setup:
        acceptor.start()
        def session = Session.lookupSession(sessionID)
        session.nextSenderMsgSeqNum = 10

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

    def "should logout when invalid logon message is sent 1B-d-2-4"() {
        setup:
        def group = new NioEventLoopGroup(1)
        def sendingHandler = new MessageSendingHandler(["8=${sessionID.beginString}\u00019=55\u000135=A\u000149=${sessionID.senderCompID}\u000156=${sessionID.targetCompID}\u000110=087\u0001"])
        def channel = new ServerBootstrap().channel(NioServerSocketChannel).
                group(group).
                childHandler(sendingHandler).
                bind(port).
                syncUninterruptibly().
                channel()

        when:
        engine.start()
        while (!sendingHandler.becameInactive) {
            Thread.sleep(500)
        }

        then:
        !sessionSateListener.loggedOn
        sessionSateListener.sessionState == null
        sendingHandler.becameInactive
        sendingHandler.receivedMessages.size() == 3
        Logon logon = new Logon()
        logon.fromString(sendingHandler.receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Reject reject = new Reject()
        reject.fromString(sendingHandler.receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        reject.getText().value == "Invalid logon message"
        reject.getSessionRejectReason().value == SessionRejectReason.OTHER
        Logout logout = new Logout()
        logout.fromString(sendingHandler.receivedMessages[2], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        logout.getText().value == "Invalid logon message"

        cleanup:
        group?.shutdownGracefully()?.syncUninterruptibly()
    }

    def "should disconnect if first message is not a logon tests case 1B-e-4"() {
        setup:
        def group = new NioEventLoopGroup(1)
        def messageSendingHandler = new MessageSendingHandler(["8=${sessionID.beginString}\u00019=55\u000135=1\u000149=${sessionID.senderCompID}\u000156=${sessionID.targetCompID}\u000110=087\u0001"])
        def channel = new ServerBootstrap().channel(NioServerSocketChannel).
                group(group).
                childHandler(messageSendingHandler).
                bind(port).
                syncUninterruptibly().
                channel()

        when:
        engine.start()
        while (!messageSendingHandler.becameInactive) {
            Thread.sleep(500)
        }

        then:
        messageSendingHandler.becameInactive
        !sessionSateListener.loggedOn
        sessionSateListener.sessionState == null

        cleanup:
        group?.shutdownGracefully()?.syncUninterruptibly()
    }

    private static class MessageSendingHandler extends ChannelInboundHandlerAdapter {

        private boolean becameInactive
        private List<String> receivedMessages = []
        private List<String> messagesToSend

        private MessageSendingHandler(List<String> messagesToSend) {
            this.messagesToSend = messagesToSend
        }

        @Override
        void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx)
            becameInactive = true
        }

        @Override
        void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof ByteBuf) {
                this.receivedMessages.addAll(AbstractFixYOUAcceptorIntegrationTest.splitMessagesIfNecessary(msg.readCharSequence(msg.readableBytes(), StandardCharsets.US_ASCII)))
            }
            if (!messagesToSend.isEmpty()) {
                ctx.writeAndFlush(Unpooled.wrappedBuffer(messagesToSend.remove(0).getBytes(StandardCharsets.US_ASCII)))
            }
        }
    }

}
