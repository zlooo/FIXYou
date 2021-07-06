package io.github.zlooo.fixyou.netty.handler.validation

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.fix.commons.RejectReasons
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.netty.SimpleFixMessage
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import spock.lang.Specification

import java.time.LocalDateTime
import java.time.ZoneOffset

class SimpleValidatorsTest extends Specification {

    def "should check if original sending time is set when possible dup flag is set"() {
        expect:
        SimpleValidators.ORIG_SENDING_TIME_PRESENT.apply(createFixMessage(origSendingTime, possDupFlag)) == null

        where:
        possDupFlag | origSendingTime
        true        | LocalDateTime.now()
        false       | LocalDateTime.now()
        false       | null
    }

    def "should allow original sending time to be absent when possible dup flag is not set"() {
        expect:
        SimpleValidators.ORIG_SENDING_TIME_PRESENT.apply(createFixMessage(origSendingTime, possDupFlag)) == null

        where:
        possDupFlag | origSendingTime
        null        | LocalDateTime.now()
        null        | null
    }

    def "should fail validation if original sending time is empty and possible dup flag is set"() {
        expect:
        SimpleValidators.ORIG_SENDING_TIME_PRESENT.apply(createFixMessage(null, true)) != null
    }

    def "should send reject message when original sending time is empty and possible dup flag is set"() {
        setup:
        FixMessage fixMessage = createFixMessage(null, true)
        ChannelHandlerContext channelHandlerContext = Mock()
        ChannelFuture channelFuture = Mock()

        when:
        SimpleValidators.ORIG_SENDING_TIME_PRESENT.apply(fixMessage).perform(channelHandlerContext, fixMessage, null)

        then:
        1 * channelHandlerContext.writeAndFlush(fixMessage) >> channelFuture
        1 * channelFuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
        fixMessage.getCharSequenceValue(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).toString() == String.valueOf(FixConstants.REJECT)
        fixMessage.getLongValue(FixConstants.SESSION_REJECT_REASON_FIELD_NUMBER) == RejectReasons.REQUIRED_TAG_MISSING
        fixMessage.getLongValue(FixConstants.REFERENCED_TAG_ID_FIELD_NUMBER) == FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER
        0 * _
    }

    private static FixMessage createFixMessage(LocalDateTime origSendingTime, Boolean possDupFlag) {
        def fixMessage = new SimpleFixMessage()
        if (origSendingTime != null) {
            fixMessage.setTimestampValue(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER, origSendingTime.toInstant(ZoneOffset.UTC).toEpochMilli())
        }
        if (possDupFlag != null) {
            fixMessage.setBooleanValue(FixConstants.POSSIBLE_DUPLICATE_FLAG_FIELD_NUMBER, possDupFlag)
        }
        return fixMessage
    }
}
