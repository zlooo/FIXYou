package pl.zlooo.fixyou.netty.utils

import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.util.Attribute
import pl.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import pl.zlooo.fixyou.session.SessionConfig
import pl.zlooo.fixyou.session.SessionStateListener
import spock.lang.Specification

class FixChannelListenersTest extends Specification {

    private ChannelFuture channelFuture = Mock()
    private Channel channel = Mock()
    private Attribute<NettyHandlerAwareSessionState> sessionStateAttribute = Mock()
    private NettyHandlerAwareSessionState sessionState = Mock()

    def "should mark logout as sent and notify listeners if future is successful"() {
        setup:
        def sessionConfig = new SessionConfig()
        def stateListener = Mock(SessionStateListener)
        sessionConfig.addSessionStateListener(stateListener)

        when:
        FixChannelListeners.LOGOUT_SENT.operationComplete(channelFuture)

        then:
        1 * channelFuture.isSuccess() >> true
        1 * channelFuture.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        1 * sessionState.setLogoutSent(true)
        1 * sessionState.getSessionConfig() >> sessionConfig
        1 * stateListener.logOut(sessionState)
        0 * _
    }

    def "should not mark logout as sent if future is successful"() {
        when:
        FixChannelListeners.LOGOUT_SENT.operationComplete(channelFuture)

        then:
        1 * channelFuture.isSuccess() >> false
        0 * _
    }

    def "should mark logon as sent if future is successful"() {
        when:
        FixChannelListeners.LOGON_SENT.operationComplete(channelFuture)

        then:
        1 * channelFuture.isSuccess() >> true
        1 * channelFuture.channel() >> channel
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionStateAttribute
        1 * sessionStateAttribute.get() >> sessionState
        1 * sessionState.setLogonSent(true)
        0 * _
    }

    def "should not logon logout as sent if future is successful"() {
        when:
        FixChannelListeners.LOGON_SENT.operationComplete(channelFuture)

        then:
        1 * channelFuture.isSuccess() >> false
        0 * _
    }
}
