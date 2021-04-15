package io.github.zlooo.fixyou.netty.handler.admin

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.commons.pool.DefaultObjectPool
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.time.Instant

class RetransmitionSubscriberTest extends Specification {

    private ChannelHandlerContext channelHandlerContext = Mock()
    private DefaultObjectPool<FixMessage> fixMessagePool = Mock()
    private RetransmitionSubscriber fixMessageSubscriber = new RetransmitionSubscriber()
    private DefaultObjectPool<RetransmitionSubscriber> fixMessageSubscriberPool = Mock()
    private ChannelFuture channelFuture = Mock()
    @AutoCleanup
    private FixMessage fixMessage = new NotPoolableFixMessage()

    void setup() {
        fixMessageSubscriber.channelHandlerContext = channelHandlerContext
        fixMessageSubscriber.fixMessagePool = fixMessagePool
        fixMessageSubscriber.pool = fixMessageSubscriberPool
    }

    def "should check if mandatory fields are set when subscription begins"() {
        setup:
        fixMessageSubscriber.channelHandlerContext = ctx
        fixMessageSubscriber.fixMessagePool = fmp

        when:
        fixMessageSubscriber.onSubscribe()

        then:
        thrown(IllegalStateException)
        0 * _

        where:
        ctx                         | fmp
        null                        | Mock(DefaultObjectPool)
        Mock(ChannelHandlerContext) | null
        null                        | null
    }

    def "should retransmit non-admin message"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        def sendingTime = Instant.now().toEpochMilli()
        fixMessage.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, sendingTime)

        when:
        fixMessageSubscriber.onNext(1, fixMessage)

        then:
        fixMessage.getBooleanValue(FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER)
        fixMessage.getTimestampValue(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER) == sendingTime
        fixMessage.refCnt() == 1
        1 * channelHandlerContext.write(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        0 * _
    }

    def "should retransmit non-admin message proceeded by gap fill"() {
        setup:
        FixMessage gapFill = new NotPoolableFixMessage()
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "D")
        def sendingTime = Instant.now().toEpochMilli()
        fixMessage.setTimestampValue(FixConstants.SENDING_TIME_FIELD_NUMBER, sendingTime)
        fixMessageSubscriber.@fromValue = 666
        fixMessageSubscriber.@toValue = 777

        when:
        fixMessageSubscriber.onNext(778, fixMessage)

        then:
        1 * fixMessagePool.getAndRetain() >> gapFill
        gapFill.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 666L
        gapFill.getLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER) == 778L
        1 * channelHandlerContext.write(gapFill)
        fixMessage.getBooleanValue(FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER)
        fixMessage.getTimestampValue(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER) == sendingTime
        fixMessage.refCnt() == 1
        1 * channelHandlerContext.write(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        0 * _

        cleanup:
        gapFill?.close()
    }

    def "should not retransmit admin message and store sequence number when state is blank"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "0")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 666)

        when:
        fixMessageSubscriber.onNext(666, fixMessage)

        then:
        fixMessageSubscriber.@fromValue == 666L
        fixMessageSubscriber.@toValue == 666L
        fixMessage.refCnt() == 0
        0 * _
    }

    def "should not retransmit admin message and store sequence number when previous sequence is stored"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "0")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 666)
        fixMessageSubscriber.@fromValue = 665
        fixMessageSubscriber.@toValue = 665

        when:
        fixMessageSubscriber.onNext(666, fixMessage)

        then:
        fixMessageSubscriber.@fromValue == 665L
        fixMessageSubscriber.@toValue == 666L
        fixMessage.refCnt() == 0
        0 * _
    }

    def "should store sequence number for gap fill if message is corrupt"() {
        setup:
        fixMessage.setCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER, "0")
        fixMessage.setLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, 666)

        when:
        fixMessageSubscriber.onNext(666, fixMessage)

        then:
        fixMessageSubscriber.@fromValue == 666L
        fixMessageSubscriber.@toValue == 666L
        fixMessage.refCnt() == 0
        0 * _
    }

    def "should store sequence number for gap fill if provided message is empty"() {
        when:
        fixMessageSubscriber.onNext(666, FixMessageUtils.EMPTY_FAKE_MESSAGE)

        then:
        fixMessageSubscriber.@fromValue == 666L
        fixMessageSubscriber.@toValue == 666L
        0 * _
    }

    def "should push gap fill if queued, flush and release on error"() {
        setup:
        fixMessageSubscriber.retain()
        fixMessageSubscriber.@fromValue = 666L
        fixMessageSubscriber.@toValue = 777L
        FixMessage gapFill = new NotPoolableFixMessage()

        when:
        fixMessageSubscriber.onError(new Exception())

        then:
        1 * fixMessagePool.getAndRetain() >> gapFill
        gapFill.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 666L
        gapFill.getLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER) == 778L
        gapFill.refCnt() == 1
        1 * channelHandlerContext.write(gapFill)
        1 * channelHandlerContext.flush()
        fixMessageSubscriberPool.returnObject(fixMessageSubscriber)
        fixMessageSubscriber.refCnt() == 0

        cleanup:
        gapFill?.close()
    }

    def "should push gap fill if queued, flush and release on subscription completion"() {
        setup:
        fixMessageSubscriber.retain()
        fixMessageSubscriber.@fromValue = 666L
        fixMessageSubscriber.@toValue = 777L
        FixMessage gapFill = new NotPoolableFixMessage()

        when:
        fixMessageSubscriber.onComplete()

        then:
        1 * fixMessagePool.getAndRetain() >> gapFill
        gapFill.getLongValue(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER) == 666L
        gapFill.getLongValue(FixConstants.NEW_SEQUENCE_NUMBER_FIELD_NUMBER) == 778L
        gapFill.refCnt() == 1
        1 * channelHandlerContext.write(gapFill)
        1 * channelHandlerContext.flush()
        fixMessageSubscriberPool.returnObject(fixMessageSubscriber)
        fixMessageSubscriber.refCnt() == 0

        cleanup:
        gapFill?.close()
    }
}
