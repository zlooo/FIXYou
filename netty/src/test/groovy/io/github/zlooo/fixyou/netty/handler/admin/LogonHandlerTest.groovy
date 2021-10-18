package io.github.zlooo.fixyou.netty.handler.admin

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.Resettable
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.fix.commons.LogoutTexts
import io.github.zlooo.fixyou.model.ApplicationVersionID
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState
import io.github.zlooo.fixyou.netty.SimpleFixMessage
import io.github.zlooo.fixyou.netty.handler.Handlers
import io.github.zlooo.fixyou.netty.handler.MutableIdleStateHandler
import io.github.zlooo.fixyou.netty.handler.NettyResettablesNames
import io.github.zlooo.fixyou.netty.handler.SessionAwareChannelInboundHandler
import io.github.zlooo.fixyou.netty.utils.DelegatingChannelHandlerContext
import io.github.zlooo.fixyou.netty.utils.FixChannelListeners
import io.github.zlooo.fixyou.netty.utils.PipelineUtils
import io.github.zlooo.fixyou.session.*
import io.netty.channel.*
import io.netty.util.Attribute
import io.netty.util.concurrent.EventExecutor
import io.netty.util.concurrent.ScheduledFuture
import spock.lang.Specification

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

class LogonHandlerTest extends Specification {

    public static final Instant NOW = Instant.parse("2021-10-10T10:15:30.574Z")
    //Sunday
    private io.github.zlooo.fixyou.fix.commons.Authenticator authenticator = Mock()
    private SessionRegistry sessionRegistry = Mock()
    private ChannelHandler preMessageValidatorHandler = Mock()
    private ChannelHandler postMessageValidatorHandler = Mock()
    private DefaultObjectPool<FixMessage> fixMessageObjectPool = Mock()
    private LogonHandler logonHandler = new LogonHandler(authenticator, sessionRegistry, preMessageValidatorHandler, postMessageValidatorHandler, fixMessageObjectPool, Clock.fixed(NOW, ZoneOffset.UTC))
    private ChannelHandlerContext channelHandlerContext = Mock()
    private Channel channel = Mock()
    private SessionID sessionID = new SessionID("beginString", "senderCompId", "targetCompId")
    private SimpleFixMessage fixMessage = createValidLogonMessage(sessionID)
    private SessionStateListener sessionStateListener = Mock()
    private NettyHandlerAwareSessionState sessionState = new NettyHandlerAwareSessionState(SessionConfig.
            builder().
            validationConfig(ValidationConfig.builder().validate(true).build()).
            consolidateFlushes(false).
            sessionStateListener(sessionStateListener).
            build(), sessionID, TestSpec.INSTANCE) {

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
    private ChannelHandlerContext sessionHandlerContext = Mock()

    void setup() {
        sessionState.resettables.putAll([(NettyResettablesNames.MESSAGE_DECODER)                                             : messageDecoder,
                                         (NettyResettablesNames.SESSION)                                                     : sessionHandler,
                                         (NettyResettablesNames.NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT): nmfCtx,
                                         (NettyResettablesNames.IDLE_STATE_HANDLER)                                          : idleStateHandler] as Map<? extends String, ? extends Resettable>)
    }

    def "should close channel when logon on existing session is received"() {
        setup:
        SessionID sessionID = new SessionID("beginString", "targetCompId", "senderCompId")
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
        SessionID sessionID = new SessionID("beginString", "targetCompId", "senderCompId")

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
        SessionID sessionID = new SessionID("beginString", "targetCompId", "senderCompId")
        ChannelFuture channelFuture = Mock()

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * authenticator.isAuthenticated(fixMessage) >> false
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture
        fixMessage.refCnt() == 1 //+1 since fix message is being used to send logout
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGOUT
        fixMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).chars == LogoutTexts.BAD_CREDENTIALS
        sessionState.channel == null
        0 * _
    }

    def "should start fix session"() {
        setup:
        SessionID sessionID = new SessionID("beginString", "targetCompId", "senderCompId")
        ChannelPipeline channelPipeline = Mock()
        Attribute sessionAttribute = Mock()
        ChannelFuture channelFuture = Mock()
        FixMessage logonResponse = new SimpleFixMessage()
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
        1 * fixMessageObjectPool.getAndRetain() >> logonResponse
        1 * channelHandlerContext.writeAndFlush(logonResponse) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGON_SENT) >> channelFuture
        logonResponse.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGON
        logonResponse.getLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER) == 0L
        logonResponse.getLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER) == 15L
        logonResponse.getCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).chars == ApplicationVersionID.FIX50SP2.value
        1 * sessionStateListener.logOn(sessionState)
        1 * sessionHandler.getSessionState() >> sessionState
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.context(Handlers.SESSION.getName()) >> sessionHandlerContext
        1 * nmfCtx.setDelegate(sessionHandlerContext) >> nmfCtx
        1 * sessionHandler.channelRead(nmfCtx, fixMessage)
        sessionState.channel == channel
        0 * _

        cleanup:
        logonResponse?.close()
    }

    def "should start fix session and reset it when reset seq number flag is set"() {
        setup:
        SessionID sessionID = new SessionID("beginString", "targetCompId", "senderCompId")
        ChannelPipeline channelPipeline = Mock()
        Attribute sessionAttribute = Mock()
        ChannelFuture channelFuture = Mock()
        FixMessage logonResponse = new SimpleFixMessage()
        sessionState.logonSent = false
        fixMessage.setBooleanValue(FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER, true)
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 1L)

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
        1 * fixMessageObjectPool.getAndRetain() >> logonResponse
        1 * channelHandlerContext.writeAndFlush(logonResponse) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGON_SENT) >> channelFuture
        logonResponse.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGON
        logonResponse.getLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER) == 0L
        logonResponse.getLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER) == 15L
        logonResponse.getCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).chars == ApplicationVersionID.FIX50SP2.value
        1 * sessionStateListener.logOn(sessionState)
        1 * sessionHandler.getSessionState() >> sessionState
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.context(Handlers.SESSION.getName()) >> sessionHandlerContext
        1 * nmfCtx.setDelegate(sessionHandlerContext) >> nmfCtx
        1 * sessionHandler.channelRead(nmfCtx, fixMessage)
        sessionState.channel == channel
        0 * _

        cleanup:
        logonResponse?.close()
    }

    def "should close channel when exception occurs during sequence number for logon message check"() {
        setup:
        SessionID sessionID = new SessionID("beginString", "targetCompId", "senderCompId")
        ChannelPipeline channelPipeline = Mock()
        Attribute sessionAttribute = Mock()
        fixMessage.setLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, 0L)
        fixMessage.setLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, 15L)
        fixMessage.setCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, ApplicationVersionID.FIX50SP2.value)
        ChannelFuture channelFuture = Mock()
        FixMessage logonResponse = new SimpleFixMessage()
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
        1 * fixMessageObjectPool.getAndRetain() >> logonResponse
        1 * channelHandlerContext.writeAndFlush(logonResponse) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGON_SENT) >> channelFuture
        logonResponse.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGON
        logonResponse.getLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER) == 0L
        logonResponse.getLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER) == 15L
        logonResponse.getCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).chars == ApplicationVersionID.FIX50SP2.value
        1 * sessionStateListener.logOn(sessionState)
        1 * sessionHandler.getSessionState() >> sessionState
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.context(Handlers.SESSION.getName()) >> sessionHandlerContext
        1 * nmfCtx.setDelegate(sessionHandlerContext) >> nmfCtx
        1 * sessionHandler.channelRead(nmfCtx, fixMessage) >> { args -> throw new RuntimeException("test exception") }
        1 * channelHandlerContext.close()
        sessionState.channel == channel
        0 * _

        cleanup:
        logonResponse?.close()
    }

    def "should start fix session but not send logon because it has already been sent"() {
        setup:
        SessionID sessionID = new SessionID("beginString", "targetCompId", "senderCompId")
        ChannelPipeline channelPipeline = Mock()
        Attribute sessionAttribute = Mock()
        fixMessage.setLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, 0L)
        fixMessage.setLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, 15L)
        fixMessage.setCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, ApplicationVersionID.FIX50SP2.value)
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
        SessionID sessionID = new SessionID("beginString", "targetCompId", "senderCompId")
        ChannelPipeline channelPipeline = Mock()
        fixMessage.setLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, 0L)
        fixMessage.setLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, 15L)
        fixMessage.setCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, ApplicationVersionID.FIX50SP2.value)
        ChannelFuture channelFuture = Mock()
        FixMessage logonResponse = new SimpleFixMessage()
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
        1 * fixMessageObjectPool.getAndRetain() >> logonResponse
        1 * channelHandlerContext.writeAndFlush(logonResponse) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGON_SENT) >> channelFuture
        logonResponse.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGON
        logonResponse.getLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER) == 0L
        logonResponse.getLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER) == 15L
        logonResponse.getCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).chars == ApplicationVersionID.FIX50SP2.value
        1 * sessionStateListener.logOn(sessionState)
        1 * channelHandlerContext.channel() >> channel
        sessionState.channel == channel
        0 * _

        cleanup:
        logonResponse?.close()
    }

    def "should send reject and logout when invalid logon message is received"() {
        setup:
        SessionID sessionID = new SessionID("beginString", "targetCompId", "senderCompId")
        fixMessage.removeField(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER)
        ChannelOutboundHandler sessionOutChannelHandler = Mock()
        sessionState.getResettables().put(NettyResettablesNames.SESSION, sessionOutChannelHandler)
        FixMessage reject = new SimpleFixMessage()
        ChannelFuture rejectMessageFuture = Mock()
        ChannelFuture logoutMessageFuture = Mock()

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * fixMessageObjectPool.getAndRetain() >> reject
        1 * sessionOutChannelHandler.write(nmfCtx, reject, null)
        1 * channelHandlerContext.write(reject) >> rejectMessageFuture
        1 * rejectMessageFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        1 * sessionOutChannelHandler.write(nmfCtx, fixMessage, null)
        fixMessage.refCnt() == 1 // +1 because fixMessage is being reused as logout
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> logoutMessageFuture
        1 * logoutMessageFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> logoutMessageFuture
        1 * logoutMessageFuture.addListener(ChannelFutureListener.CLOSE)
        0 * _

        cleanup:
        reject?.close()
    }

    def "should schedule session logout when session is not infinite"() {
        setup:
        sessionState = new NettyHandlerAwareSessionState(
                SessionConfig.builder().validationConfig(ValidationConfig.builder().validate(true).build()).consolidateFlushes(false).sessionStateListener(sessionStateListener)
                             .startStopConfig(StartStopConfig.builder().startTime(startTime).startDay(startDay).stopTime(stopTime).stopDay(stopDay).build()).build(),
                sessionID,
                TestSpec.INSTANCE)
        setup()
        SessionID sessionID = new SessionID("beginString", "targetCompId", "senderCompId")
        ChannelPipeline channelPipeline = Mock()
        Attribute sessionAttribute = Mock()
        ChannelFuture channelFuture = Mock()
        ChannelFuture channelFuture2 = Mock()
        FixMessage logonResponse = new SimpleFixMessage()
        sessionState.logonSent = false
        sessionState.logoutSent = false
        FixMessage logoutMessage = new SimpleFixMessage()
        EventExecutor executor = Mock()

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 0
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * channelHandlerContext.executor() >> executor
        1 * executor.schedule(_ as Callable, scheduleTime, TimeUnit.MILLISECONDS) >> { args ->
            args[0].call()
            return Mock(ScheduledFuture)
        }
        logoutMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGOUT
        !logoutMessage.isValueSet(FixConstants.TEXT_FIELD_NUMBER)
        1 * channelHandlerContext.writeAndFlush(logoutMessage) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture2
        1 * channelFuture2.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture2
        1 * authenticator.isAuthenticated(fixMessage) >> true
        !sessionState.logoutSent //! because FixChannelListeners.LOGOUT_SENT has not been executed
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.get(Handlers.SESSION.getName())
        2 * channelHandlerContext.channel() >> channel
        interaction {
            sessionHandlersAddedToPipelineAssertions(channelPipeline, sessionAttribute, 15)
        }
        2 * fixMessageObjectPool.getAndRetain() >> logoutMessage >> logonResponse
        1 * channelHandlerContext.writeAndFlush(logonResponse) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture
        1 * channelFuture.addListener(FixChannelListeners.LOGON_SENT) >> channelFuture
        logonResponse.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGON
        logonResponse.getLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER) == 0L
        logonResponse.getLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER) == 15L
        logonResponse.getCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER).chars == ApplicationVersionID.FIX50SP2.value
        1 * sessionStateListener.logOn(sessionState)
        1 * sessionHandler.getSessionState() >> sessionState
        1 * channelHandlerContext.pipeline() >> channelPipeline
        1 * channelPipeline.context(Handlers.SESSION.getName()) >> sessionHandlerContext
        1 * nmfCtx.setDelegate(sessionHandlerContext) >> nmfCtx
        1 * sessionHandler.channelRead(nmfCtx, fixMessage)
        sessionState.channel == channel
        0 * _

        where:
        startTime                                                | startDay           | stopTime                                                 | stopDay           | scheduleTime
        NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(1)  | null               | NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(21) | null              | NOW.atOffset(ZoneOffset.UTC).withHour(21).toInstant().toEpochMilli() -
        NOW.toEpochMilli()
        NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(16) | null               | NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(15) | null              |
        NOW.atOffset(ZoneOffset.UTC).withHour(15).plusDays(1).toInstant().toEpochMilli() - NOW.toEpochMilli()
        NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(1)  | DayOfWeek.SATURDAY | NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(21) | DayOfWeek.SUNDAY  |
        NOW.atOffset(ZoneOffset.UTC).withHour(21).toInstant().toEpochMilli() - NOW.toEpochMilli()
        NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(1)  | DayOfWeek.SATURDAY | NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(21) | DayOfWeek.TUESDAY |
        NOW.atOffset(ZoneOffset.UTC).withHour(21).plusDays(2).toInstant().toEpochMilli() - NOW.toEpochMilli()
    }

    def "should logout immediately when session is started outside active time"() {
        setup:
        sessionState = new NettyHandlerAwareSessionState(
                SessionConfig.builder().validationConfig(ValidationConfig.builder().validate(true).build()).consolidateFlushes(false).sessionStateListener(sessionStateListener)
                             .startStopConfig(StartStopConfig.builder().startTime(startTime).startDay(startDay).stopTime(stopTime).stopDay(stopDay).build()).build(),
                sessionID,
                TestSpec.INSTANCE)
        setup()
        SessionID sessionID = new SessionID("beginString", "targetCompId", "senderCompId")
        ChannelFuture channelFuture2 = Mock()
        sessionState.logonSent = false
        sessionState.logoutSent = false
        FixMessage logoutMessage = new SimpleFixMessage()

        when:
        logonHandler.handleMessage(fixMessage, channelHandlerContext)

        then:
        fixMessage.refCnt() == 0
        1 * sessionRegistry.getStateForSession(sessionID) >> sessionState
        1 * fixMessageObjectPool.getAndRetain() >> logoutMessage
        logoutMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).chars == FixConstants.LOGOUT
        logoutMessage.getCharSequenceValue(FixConstants.TEXT_FIELD_NUMBER).chars == LogoutTexts.LOGON_OUTSIDE_SESSION_ACTIVE_TIME
        1 * channelHandlerContext.writeAndFlush(logoutMessage) >> channelFuture2
        1 * channelFuture2.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE) >> channelFuture2
        1 * channelFuture2.addListener(FixChannelListeners.LOGOUT_SENT) >> channelFuture2
        !sessionState.logoutSent //! because FixChannelListeners.LOGOUT_SENT has not been executed
        0 * _

        where:
        startTime                                                | startDay         | stopTime                                                 | stopDay           | scheduleTime
        NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(1)  | DayOfWeek.MONDAY | NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(21) | DayOfWeek.TUESDAY |
        NOW.atOffset(ZoneOffset.UTC).withHour(21).plusDays(2).toInstant().toEpochMilli() - NOW.toEpochMilli()
        NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(12) | null             | NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(21) | null              |
        NOW.atOffset(ZoneOffset.UTC).withHour(21).toInstant().toEpochMilli() - NOW.toEpochMilli()
        NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(12) | DayOfWeek.SUNDAY | NOW.atOffset(ZoneOffset.UTC).toOffsetTime().withHour(21) | DayOfWeek.FRIDAY  |
        NOW.atOffset(ZoneOffset.UTC).withHour(21).toInstant().toEpochMilli() - NOW.toEpochMilli()
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

    private static SimpleFixMessage createValidLogonMessage(SessionID sessionID) {
        SimpleFixMessage logon = new SimpleFixMessage()
        logon.setCharSequenceValue(FixConstants.BEGIN_STRING_FIELD_NUMBER, sessionID.beginString)
        logon.setCharSequenceValue(FixConstants.SENDER_COMP_ID_FIELD_NUMBER, sessionID.senderCompID)
        logon.setCharSequenceValue(FixConstants.TARGET_COMP_ID_FIELD_NUMBER, sessionID.targetCompID)
        logon.setLongValue(FixConstants.BODY_LENGTH_FIELD_NUMBER, 666L)
        logon.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 666L)
        logon.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.LOGON)
        logon.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, Instant.now().toEpochMilli())
        logon.setLongValue(FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, 0L)
        logon.setLongValue(FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, 15L)
        logon.setCharSequenceValue(FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, ApplicationVersionID.FIX50SP2.value)
        return logon
    }
}
