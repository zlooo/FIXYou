package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.fix.commons.session.MemoryMessageStore
import io.github.zlooo.fixyou.netty.test.framework.FixMessages
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.LongSubscriber
import io.github.zlooo.fixyou.session.MessageStore
import io.github.zlooo.fixyou.session.SessionConfig
import org.assertj.core.api.Assertions
import quickfix.field.ClOrdID
import quickfix.fixt11.Message
import spock.lang.Timeout

import java.util.function.Function

@Timeout(10)
class QueueOutgoingMessagesIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    private MessageStore<FixMessage> messageStore = new MemoryMessageStore()

    @Override
    protected SessionConfig createConfig() {
        return super.createConfig().setPersistent(true).setMessageStore(messageStore)
    }

    def "should queue outgoing messages 16-a"() {
        setup:
        TestSub testSub = new TestSub()
        def clordid = UUID.randomUUID()

        when:
        def future = FIXYouNetty.sendMessage(FixMessages.createFIXYouNewOrderSingle(clordid), fixYouSessionId, engine)
        pollingConditions.eventually {
            future.done
        }

        then:
        future.get() == null //in reality we're checking if future is successful
        messageStore.getMessages(fixYouSessionId, 1, 1, testSub)
        Assertions.assertThat(testSub.messages).hasSize(1)
        testSub.started
        testSub.done
        testSub.throwable == null
        def fixMessage = testSub.messages.get(1L)
        fixMessage.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value == 1
        fixMessage.getField(FixConstants.BEGIN_STRING_FIELD_NUMBER).value.toString() == String.valueOf(fixYouSessionId.beginString)
        fixMessage.getField(FixConstants.SENDER_COMP_ID_FIELD_NUMBER).value.toString() == String.valueOf(fixYouSessionId.senderCompID)
        fixMessage.getField(FixConstants.TARGET_COMP_ID_FIELD_NUMBER).value.toString() == String.valueOf(fixYouSessionId.targetCompID)
        fixMessage.getField(ClOrdID.FIELD).value.toString() == clordid.toString()

        cleanup:
        messageStore.@sessionToMessagesMap.get(fixYouSessionId).values().forEach({ msg -> msg.release() }) //message store retains messages, but since this is end of test we want to release all FixMessages
    }

    def "should deliver queued messages when session is established 16-b"() {
        setup:
        def clordid1 = UUID.randomUUID()
        def clordid2 = UUID.randomUUID()
        def clordid3 = UUID.randomUUID()
        FIXYouNetty.sendMessage(FixMessages.createFIXYouNewOrderSingle(clordid1), fixYouSessionId, engine)
        FIXYouNetty.sendMessage(FixMessages.createFIXYouNewOrderSingle(clordid2), fixYouSessionId, engine)
        FIXYouNetty.sendMessage(FixMessages.createFIXYouNewOrderSingle(clordid3), fixYouSessionId, engine)

        when:
        startQuickfixAndWaitTillLoggedIn()
        pollingConditions.eventually {
            testQuickfixApplication.messagesReceived.size() >= 3
        }

        then:
        Assertions.
                assertThat(testQuickfixApplication.messagesReceived).
                allMatch({ msg -> msg.getHeader().getString(FixConstants.MESSAGE_TYPE_FIELD_NUMBER) == "D" }, "message type is new order single").
                allMatch({ msg -> msg.getHeader().getUtcTimeStamp(FixConstants.SENDING_TIME_FIELD_NUMBER) == msg.getHeader().getUtcTimeStamp(FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER) }, "sending time is equal to original sending time").
                extracting((Function<Message, String>) { msg -> msg.getString(ClOrdID.FIELD) }).
                containsOnly(clordid1.toString(), clordid2.toString(), clordid3.toString())

        cleanup:
        messageStore.@sessionToMessagesMap.get(fixYouSessionId).values().forEach({ msg -> msg.release() }) //message store retains messages, but since this is end of test we want to release all FixMessages
    }

    private final static class TestSub implements LongSubscriber<FixMessage> {

        boolean started = false
        boolean done = false
        Throwable throwable = null
        Map<Long, FixMessage> messages = new HashMap<>()

        @Override
        void onSubscribe() {
            started = true
        }

        @Override
        void onNext(long key, FixMessage item) {
            messages.put(key, item)
        }

        @Override
        void onError(Throwable throwable) {
            this.throwable = throwable
        }

        @Override
        void onComplete() {
            done = true
        }
    }
}
