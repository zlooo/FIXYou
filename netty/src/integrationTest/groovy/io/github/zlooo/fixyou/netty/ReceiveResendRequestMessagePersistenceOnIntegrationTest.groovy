package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.fix.commons.session.MemoryMessageStore
import io.github.zlooo.fixyou.netty.test.framework.FixMessages
import io.github.zlooo.fixyou.netty.test.framework.TestSpec
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.MessageStore
import io.github.zlooo.fixyou.session.SessionConfig
import org.assertj.core.api.Assertions
import quickfix.Message
import quickfix.Session
import quickfix.field.*
import quickfix.fixt11.Logon
import quickfix.fixt11.ResendRequest
import quickfix.fixt11.SequenceReset
import spock.lang.Timeout

import java.util.function.Function

@Timeout(30)
class ReceiveResendRequestMessagePersistenceOnIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    private MessageStore fakeMessageStore
    private UUID clordid1 = UUID.randomUUID()
    private UUID clordid2 = UUID.randomUUID()
    private UUID clordid3 = UUID.randomUUID()

    @Override
    protected SessionConfig createConfig() {
        fakeMessageStore = new MemoryMessageStore()
        def newOrderSingle1 = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        FixMessages.createFIXYouNewOrderSingle(clordid1).accept(newOrderSingle1)
        newOrderSingle1.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 2L
        fakeMessageStore.storeMessage(fixYouSessionId, 2L, newOrderSingle1)
        def newOrderSingle2 = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        FixMessages.createFIXYouNewOrderSingle(clordid2).accept(newOrderSingle2)
        newOrderSingle2.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 3L
        fakeMessageStore.storeMessage(fixYouSessionId, 3L, newOrderSingle2)
        def heartbeat1 = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        FixMessages.createFIXYouHeartbeat().accept(heartbeat1)
        heartbeat1.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 4L
        fakeMessageStore.storeMessage(fixYouSessionId, 4L, heartbeat1)
        def heartbeat2 = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        FixMessages.createFIXYouHeartbeat().accept(heartbeat2)
        heartbeat2.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 5L
        fakeMessageStore.storeMessage(fixYouSessionId, 5L, heartbeat2)
        def newOrderSingle3 = new FixMessage(TestSpec.INSTANCE, new FieldCodec())
        FixMessages.createFIXYouNewOrderSingle(clordid3).accept(newOrderSingle3)
        newOrderSingle3.getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).longValue = 6L
        fakeMessageStore.storeMessage(fixYouSessionId, 6L, newOrderSingle3)
        return super.createConfig().setPersistent(true).setMessageStore(fakeMessageStore)
    }

    def "should fill resend request when persistence is turned on 8-a"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        ResendRequest resendRequest = new ResendRequest()
        resendRequest.set(new BeginSeqNo(2))
        resendRequest.set(new EndSeqNo(6))
        Session session = Session.lookupSession(sessionID)

        when:
        Session.sendToTarget(resendRequest, sessionID)
        pollingConditions.eventually {
            testQuickfixApplication.messagesReceived.size() == 3
        }

        then:
        session.expectedTargetNum == 7
        testQuickfixApplication.adminMessagesReceived.size() == 2
        testQuickfixApplication.adminMessagesReceived[0] instanceof Logon
        SequenceReset sequenceReset = testQuickfixApplication.adminMessagesReceived[1]
        sequenceReset.getGapFillFlag().value
        sequenceReset.getHeader().getInt(MsgSeqNum.FIELD) == 4
        sequenceReset.getNewSeqNo().value == 6
        Assertions.
                assertThat(testQuickfixApplication.messagesReceived).
                hasSize(3).
                allMatch({ msg -> msg.getHeader().getString(MsgType.FIELD) == "D" }).
                extracting((Function<Message, String>) { msg -> msg.getString(ClOrdID.FIELD) }).
                containsExactly(clordid1.toString(), clordid2.toString(), clordid3.toString())

        cleanup:
        fakeMessageStore.reset(fixYouSessionId)
    }
}
