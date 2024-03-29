package io.github.zlooo.fixyou.netty.handler.admin

import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.SimpleFixMessage
import io.github.zlooo.fixyou.session.SessionConfig
import io.github.zlooo.fixyou.session.SessionID
import io.github.zlooo.fixyou.session.SessionStateListener
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.util.Attribute
import spock.lang.Specification

class LogoutHandlerTest extends Specification {

    private LogoutHandler logoutHandler = new LogoutHandler()
    private SessionStateListener sessionStateListener = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(SessionConfig.builder().sessionStateListener(sessionStateListener).build(), new SessionID("","",""),
                                                                                           TestSpec.INSTANCE)
    private Attribute<NettyHandlerAwareSessionState> sessionStateAttribute = Mock()
    private Channel channel = Mock()
    private ChannelHandlerContext channelHandlerContext = Mock()
    private FixMessage fixMessage = new SimpleFixMessage()
    private ChannelFuture channelFuture = Mock()

    def "should send logout message if none was sent"() {
        setup:
        sessionState.setLogoutSent(false)

        when:
        logoutHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 1 //+1 because fixMessage is being reused as LogoutResponse
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
