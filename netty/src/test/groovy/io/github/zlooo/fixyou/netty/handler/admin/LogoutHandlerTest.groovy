package io.github.zlooo.fixyou.netty.handler.admin

import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import spock.lang.Specification

class LogoutHandlerTest extends Specification {

    private LogoutHandler logoutHandler = new LogoutHandler()
    private io.github.zlooo.fixyou.session.SessionStateListener sessionStateListener = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new io.github.zlooo.fixyou.session.SessionConfig().addSessionStateListener(sessionStateListener), new io.github.zlooo.fixyou.session.SessionID([] as char[], [] as char[], [] as char[]), Mock(
            DefaultObjectPool), TestSpec.INSTANCE)
    private Attribute<NettyHandlerAwareSessionState> sessionStateAttribute = Mock()
    private Channel channel = Mock()
    private ChannelHandlerContext channelHandlerContext = Mock()
    private io.github.zlooo.fixyou.parser.model.FixMessage fixMessage = new io.github.zlooo.fixyou.parser.model.FixMessage(TestSpec.INSTANCE)
    private ChannelFuture channelFuture = Mock()

    def "should send logout message if none was sent"() {
        setup:
        sessionState.setLogoutSent(false)

        when:
        logoutHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 1
        1 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        sessionState.isLogoutSent()
        sessionState.connected
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.CLOSE) >> channelFuture
        1 * sessionStateListener.logOut(sessionState)
        0 * _
    }

    def "should close connection if logout message has already been sent"() {
        setup:
        sessionState.setLogoutSent(true)

        when:
        logoutHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 0
        2 * channelHandlerContext.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        sessionState.isLogoutSent()
        1 * sessionStateListener.logOut(sessionState)
        1 * channel.close() >> channelFuture
        0 * _
    }
}
