package io.github.zlooo.fixyou.netty.handler.admin

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.Resettable
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.fix.commons.LogoutTexts
import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.handler.Handlers
import io.github.zlooo.fixyou.netty.handler.MutableIdleStateHandler
import io.github.zlooo.fixyou.netty.handler.NettyResettablesNames
import io.github.zlooo.fixyou.netty.handler.SessionAwareChannelInboundHandler
import io.github.zlooo.fixyou.netty.utils.DelegatingChannelHandlerContext
import io.github.zlooo.fixyou.netty.utils.FixChannelListeners
import io.github.zlooo.fixyou.netty.utils.PipelineUtils
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.*
import io.netty.channel.*
import io.netty.util.Attribute
import spock.lang.Specification

import java.time.Instant
import java.util.concurrent.TimeUnit

class LogonHandlerTest extends Specification {

    private io.github.zlooo.fixyou.fix.commons.Authenticator authenticator = Mock()
    private SessionRegistry sessionRegistry = Mock()
    private ChannelHandler preMessageValidatorHandler = Mock()
    private ChannelHandler postMessageValidatorHandler = Mock()
    private LogonHandler logonHandler = new LogonHandler(authenticator, sessionRegistry, preMessageValidatorHandler, postMessageValidatorHandler)
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private DefaultObjectPool<FixMessage> fixMessageObjectReadPool = Mock()
    private DefaultObjectPool<FixMessage> fixMessageObjectWritePool = Mock()
    private SessionID sessionID = new SessionID("beginString".toCharArray(), 11, "senderCompId".toCharArray(), 12, "targetCompId".toCharArray(), 12)
    private FixMessage fixMessage = createValidLogonMessage(sessionID)
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(new SessionConfig().setValidationConfig(new ValidationConfig().setValidate(true)).setConsolidateFlushes(false), sessionID, fixMessageObjectReadPool,
                                                                                           fixMessageObjectWritePool, TestSpec.INSTANCE) {

        private boolean resetCalled

        @Override
        void reset() {
            resetCalled = true
        }
    }
    private ChannelHandler messageDecoder = Mock(additionalInterfaces: [Resettable])
    private ChannelHandler genericDecoder = Mock(additionalInterfaces: [Resettable])
    private ChannelHandler adminMessageHandler = Mock(additionalInterfaces: [Resettable])
    private ChannelHandler genericHandler = Mock(additionalInterfaces: [Resettable])
    private SessionAwareChannelInboundHandler sessionHandler = Mock(additionalInterfaces: [Resettable])
    private MutableIdleStateHandler idleStateHandler = Mock(additionalInterfaces: [Resettable])
    private DelegatingChannelHandlerContext nmfCtx = Mock(additionalInterfaces: [Resettable])
    private SessionStateListener sessionStateListener = Mock()
    private ChannelHandlerContext sessionHandlerContext = Mock()

    void setup() {
        sessionState.resettables.putAll([(NettyResettablesNames.MESSAGE_DECODER)                                             : messageDecoder,
                                         (NettyResettablesNames.SESSION)                                                     : sessionHandler,
                                         (NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT): nmfCtx,
                                         (NettyResettablesNames.IDLE_STATE_HANDLER)                                          : idleStateHandler] as Map<? extends String, ? extends Resettable>)
        sessionState.getSessionConfig().addSessionStateListener(sessionStateListener)
    }

    def "should close channel when logon on existing session is received"() {
        setup:
        SessionID sessionID = new SessionID("beginString".toCharArray(), 11, "targetCompId".toCharArray(), 12, "senderCompId".toCharArray(), 12)
        sessionState.connected.set(true)

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 0
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * channelHandlerContext.channel() >> channel
        1 * channel.close()
        sessionState.channel == null
        0 * _
    }

    def "should close channel when logon on unknown session is received"() {
        setup:
        SessionID sessionID = new SessionID("beginString".toCharArray(), 11, "targetCompId".toCharArray(), 12, "senderCompId".toCharArray(), 12)

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 0
        1 * sessionRegistry.getStateForSession(sessionID)
        1 * channelHandlerContext.channel() >> channel
        1 * channel.close()
        sessionState.channel == null
        0 * _
    }

    def "should logout when credentials are invalid"() {
        setup:
        SessionID sessionID = new SessionID("beginString".toCharArray(), 11, "targetCompId".toCharArray(), 12, "senderCompId".toCharArray(), 12)
        ChannelFuture channelFuture = Mock()

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * authenticator.isAuthenticated(fixMessage) >> false
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture
        fixMessage.refCnt() == 1
        fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).getCharSequenceValue().toString() == String.valueOf(FixConstants.LOGOUT)
        fixMessage.getField(FixConstants.TEXT_FIELD_NUMBER).getCharSequenceValue().toString() == String.valueOf(LogoutTexts.BAD_CREDENTIALS)
        sessionState.channel == null
        0 * _
    }

    def "should start fix session"() {
        setup:
        SessionID sessionID = new SessionID("beginString".toCharArray(), 11, "targetCompId".toCharArray(), 12, "senderCompId".toCharArray(), 12)
        ChannelPipeline channelPipeline = Mock()
        Attribute sessionAttribute = Mock()
        ChannelFuture channelFuture = Mock()
        FixMessage logonResponse = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        sessionState.logonSent = false
        sessionState.logoutSent = true

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 0
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * authenticator.isAuthenticated(fixMessage) >> true
        !sessionState.logoutSent
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.get(Handlers.SESSION.getName())
        2 * channelHandlerContext.channel() >> channel
        interaction {
            sessionHandlersAddedToPipelineAssertions(channelPipeline, sessionAttribute, 15)
        }
        1 * fixMessageObjectWritePool.getAndRetain() >> logonResponse
        1 * channelHandlerContext.writeAndFlush(logonResponse) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGON_SENT) >> channelFuture
        logonResponse.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.toString() == String.valueOf(FixConstants.LOGON)
        logonResponse.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue == 0L
        logonResponse.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue == 15L
        logonResponse.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue.toString() == String.valueOf(ApplicationVersionID.FIX50SP2.value)
        1 * sessionStateListener.logOn(sessionState)
        1 * sessionHandler.getSessionState() >> sessionState
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.context(Handlers.SESSION.getName()) >> sessionHandlerContext
        1 * nmfCtx.setDelegate(sessionHandlerContext) >> nmfCtx
        1 * sessionHandler.channelRead(nmfCtx, fixMessage)
        sessionState.channel == channel
        0 * _
    }

    def "should start fix session and reset it when reset seq number flag is set"() {
        setup:
        SessionID sessionID = new SessionID("beginString".toCharArray(), 11, "targetCompId".toCharArray(), 12, "senderCompId".toCharArray(), 12)
        ChannelPipeline channelPipeline = Mock()
        Attribute sessionAttribute = Mock()
        ChannelFuture channelFuture = Mock()
        FixMessage logonResponse = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        sessionState.logonSent = false
        fixMessage.getField(FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER).booleanValue = true
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 1L

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 0
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * authenticator.isAuthenticated(fixMessage) >> true
        sessionState.resetCalled
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.get(Handlers.SESSION.getName())
        2 * channelHandlerContext.channel() >> channel
        interaction {
            sessionHandlersAddedToPipelineAssertions(channelPipeline, sessionAttribute, 15)
        }
        1 * fixMessageObjectWritePool.getAndRetain() >> logonResponse
        1 * channelHandlerContext.writeAndFlush(logonResponse) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGON_SENT) >> channelFuture
        logonResponse.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.toString() == String.valueOf(FixConstants.LOGON)
        logonResponse.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue == 0L
        logonResponse.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue == 15L
        logonResponse.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue.toString() == String.valueOf(ApplicationVersionID.FIX50SP2.value)
        1 * sessionStateListener.logOn(sessionState)
        1 * sessionHandler.getSessionState() >> sessionState
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.context(Handlers.SESSION.getName()) >> sessionHandlerContext
        1 * nmfCtx.setDelegate(sessionHandlerContext) >> nmfCtx
        1 * sessionHandler.channelRead(nmfCtx, fixMessage)
        sessionState.channel == channel
        0 * _
    }

    def "should close channel when exception occurs during sequence number for logon message check"() {
        setup:
        SessionID sessionID = new SessionID("beginString".toCharArray(), 11, "targetCompId".toCharArray(), 12, "senderCompId".toCharArray(), 12)
        ChannelPipeline channelPipeline = Mock()
        Attribute sessionAttribute = Mock()
        fixMessage.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue = 0L
        fixMessage.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue = 15L
        fixMessage.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue = ApplicationVersionID.FIX50SP2.value
        ChannelFuture channelFuture = Mock()
        FixMessage logonResponse = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        sessionState.logonSent = false

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 0
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * authenticator.isAuthenticated(fixMessage) >> true
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.get(Handlers.SESSION.getName())
        2 * channelHandlerContext.channel() >> channel
        interaction {
            sessionHandlersAddedToPipelineAssertions(channelPipeline, sessionAttribute, 15)
        }
        1 * fixMessageObjectWritePool.getAndRetain() >> logonResponse
        1 * channelHandlerContext.writeAndFlush(logonResponse) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGON_SENT) >> channelFuture
        logonResponse.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.toString() == String.valueOf(FixConstants.LOGON)
        logonResponse.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue == 0L
        logonResponse.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue == 15L
        logonResponse.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue.toString() == String.valueOf(ApplicationVersionID.FIX50SP2.value)
        1 * sessionStateListener.logOn(sessionState)
        1 * sessionHandler.getSessionState() >> sessionState
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.context(Handlers.SESSION.getName()) >> sessionHandlerContext
        1 * nmfCtx.setDelegate(sessionHandlerContext) >> nmfCtx
        1 * sessionHandler.channelRead(nmfCtx, fixMessage) >> { -> throw new RuntimeException() }
        1 * channelHandlerContext.close()
        sessionState.channel == channel
        0 * _
    }

    def "should start fix session but not send logon because it has already been sent"() {
        setup:
        SessionID sessionID = new SessionID("beginString".toCharArray(), 11, "targetCompId".toCharArray(), 12, "senderCompId".toCharArray(), 12)
        ChannelPipeline channelPipeline = Mock()
        Attribute sessionAttribute = Mock()
        fixMessage.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue = 0L
        fixMessage.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue = 15L
        fixMessage.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue = ApplicationVersionID.FIX50SP2.value
        sessionState.logonSent = true

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 0
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * authenticator.isAuthenticated(fixMessage) >> true
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.get(Handlers.SESSION.getName())
        2 * channelHandlerContext.channel() >> channel
        interaction {
            sessionHandlersAddedToPipelineAssertions(channelPipeline, sessionAttribute, 15)
        }
        1 * sessionStateListener.logOn(sessionState)
        1 * sessionHandler.getSessionState() >> sessionState
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.context(Handlers.SESSION.getName()) >> sessionHandlerContext
        1 * nmfCtx.setDelegate(sessionHandlerContext) >> nmfCtx
        1 * sessionHandler.channelRead(nmfCtx, fixMessage)
        sessionState.channel == channel
        0 * _
    }

    def "should start fix session when handlers are already set up"() {
        setup:
        SessionID sessionID = new SessionID("beginString".toCharArray(), 11, "targetCompId".toCharArray(), 12, "senderCompId".toCharArray(), 12)
        ChannelPipeline channelPipeline = Mock()
        fixMessage.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue = 0L
        fixMessage.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue = 15L
        fixMessage.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue = ApplicationVersionID.FIX50SP2.value
        ChannelFuture channelFuture = Mock()
        FixMessage logonResponse = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        sessionState.logonSent = false

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 0
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * authenticator.isAuthenticated(fixMessage) >> true
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.get(Handlers.SESSION.getName()) >> sessionHandler
        !sessionState.getResettables().isEmpty()
        1 * fixMessageObjectWritePool.getAndRetain() >> logonResponse
        1 * channelHandlerContext.writeAndFlush(logonResponse) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGON_SENT) >> channelFuture
        logonResponse.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue.toString() == String.valueOf(FixConstants.LOGON)
        logonResponse.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue == 0L
        logonResponse.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue == 15L
        logonResponse.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue.toString() == String.valueOf(ApplicationVersionID.FIX50SP2.value)
        1 * sessionStateListener.logOn(sessionState)
        1 * channelHandlerContext.channel() >> channel
        sessionState.channel == channel
        0 * _
    }

    def "should send reject and logout when invalid logon message is received"() {
        setup:
        SessionID sessionID = new SessionID("beginString".toCharArray(), 11, "targetCompId".toCharArray(), 12, "senderCompId".toCharArray(), 12)
        fixMessage.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).reset()
        ChannelOutboundHandler sessionOutChannelHandler = Mock()
        sessionState.getResettables().put(NettyResettablesNames.SESSION, sessionOutChannelHandler)
        FixMessage reject = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        ChannelFuture rejectMessageFuture = Mock()
        ChannelFuture logoutMessageFuture = Mock()

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * fixMessageObjectWritePool.getAndRetain() >> reject
        1 * sessionOutChannelHandler.write(nmfCtx, reject, null)
        1 * channelHandlerContext.write(reject) >> rejectMessageFuture
        1 * rejectMessageFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        1 * sessionOutChannelHandler.write(nmfCtx, fixMessage, null)
        fixMessage.refCnt() == 1
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> logoutMessageFuture
        1 * logoutMessageFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> logoutMessageFuture
        1 * logoutMessageFuture.addListener(ChannelFutureListener.CLOSE)
        0 * _
    }

    void sessionHandlersAddedToPipelineAssertions(ChannelPipeline channelPipeline, Attribute sessionAttribute, long heartbeatInterval) {
        1 * channel.pipeline() >> channelPipeline
        assert !sessionState.getResettables().isEmpty()
        1 * channelPipeline.get(Handlers.GENERIC_DECODER.getName()) >> genericDecoder
        1 * channelPipeline.get(Handlers.MESSAGE_DECODER.getName())
        1 * channelPipeline.replace(Handlers.GENERIC_DECODER.getName(), Handlers.MESSAGE_DECODER.getName(), messageDecoder)
        1 * channelPipeline.get(Handlers.GENERIC.getName()) >> genericHandler
        1 * channelPipeline.get(Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName())
        1 * channelPipeline.addBefore(Handlers.GENERIC.getName(), Handlers.BEFORE_SESSION_MESSAGE_VALIDATOR.getName(), preMessageValidatorHandler)
        1 * channelPipeline.get(Handlers.ADMIN_MESSAGES.getName()) >> adminMessageHandler
        3 * channelPipeline.get(Handlers.SESSION.getName()) >> null >> sessionHandler
        1 * channelPipeline.addBefore(Handlers.ADMIN_MESSAGES.getName(), Handlers.SESSION.getName(), sessionHandler)
        1 * channelPipeline.get(Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName())
        1 * channelPipeline.addAfter(Handlers.SESSION.getName(), Handlers.AFTER_SESSION_MESSAGE_VALIDATOR.getName(), postMessageValidatorHandler)
        1 * channelPipeline.get(Handlers.IDLE_STATE_HANDLER.getName())
        1 * idleStateHandler.setReaderIdleTimeNanos(TimeUnit.SECONDS.toNanos(heartbeatInterval) * PipelineUtils.TEST_REQUEST_MULTIPLIER)
        1 * idleStateHandler.setWriterIdleTimeNanos(TimeUnit.SECONDS.toNanos(heartbeatInterval))
        1 * channelPipeline.addAfter(Handlers.SESSION.getName(), Handlers.IDLE_STATE_HANDLER.getName(), idleStateHandler)
        1 * channel.attr(NettyHandlerAwareSessionState.ATTRIBUTE_KEY) >> sessionAttribute
        1 * sessionAttribute.set(sessionState)
    }

    private static FixMessage createValidLogonMessage(SessionID sessionID) {
        FixMessage logon = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        logon.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).charSequenceValue = sessionID.beginString
        logon.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).charSequenceValue = sessionID.senderCompID
        logon.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).charSequenceValue = sessionID.targetCompID
        logon.getField(FixConstants.BODY_LENGTH_FIELD_NUMBER).longValue = 666L
        logon.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 666L
        logon.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue = FixConstants.LOGON
        logon.getField(FixConstants.SENDING_TIME_FIELD_NUMBER).timestampValue = Instant.now().toEpochMilli()
        logon.getField(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER).longValue = 0L
        logon.getField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER).longValue = 15L
        logon.getField(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).charSequenceValue = ApplicationVersionID.FIX50SP2.value
        return logon
    }
}
