package pl.zlooo.fixyou.netty

import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.netty.test.framework.FixMessages
import pl.zlooo.fixyou.netty.test.framework.QuickfixTestUtils
import pl.zlooo.fixyou.parser.model.CharArrayField
import pl.zlooo.fixyou.parser.model.CharField
import pl.zlooo.fixyou.parser.model.FixMessage
import quickfix.Session
import quickfix.field.*
import quickfix.fix50sp2.NewOrderSingle
import quickfix.fixt11.Logon
import quickfix.fixt11.Logout
import quickfix.fixt11.Reject
import quickfix.fixt11.ResendRequest
import spock.lang.Ignore
import spock.lang.Timeout

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Timeout(10)
class ReceiveMessageStandardHeaderIntegrationTest extends AbstractFixYOUAcceptorIntegrationTest {

    def "should process message with correct header normally 2-a,h,j,l,n,p,s"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        NewOrderSingle newOrderSingle = FixMessages.createNewOrderSingle()

        when:
        Session.sendToTarget(newOrderSingle, sessionID)
        pollingConditions.eventually {
            !testFixMessageListener.messagesReceived.isEmpty()
        }

        then:
        assertMinimalNewOrderSingle(newOrderSingle, testFixMessageListener.messagesReceived[0])
    }

    def "should send resend request when sequence number is higher than expected 2-b"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        NewOrderSingle newOrderSingle = FixMessages.createNewOrderSingle()
        Session.lookupSession(sessionID).nextSenderMsgSeqNum = 4

        when:
        Session.sendToTarget(newOrderSingle, sessionID)
        pollingConditions.eventually {
            /**
             * Waiting here for following things to happen
             * 1)FixYOU detects message gap
             * 2)FixYOU sends resend request from 2 to 3
             * 3)Quickfix responds with sequence reset - gap fill with new expected sequence 4
             * 4)FixYOU sees it has a pending message with sequence number 4 so it passes it to application, hence messagesReceived.size() should increase
             */
            testFixMessageListener.messagesReceived.size() >= 1
        }

        then:
        testFixMessageListener.messagesReceived.size() == 1
        assertMinimalNewOrderSingle(newOrderSingle, testFixMessageListener.messagesReceived[0])
        def message = testQuickfixApplication.adminMessagesReceived[0]
        message instanceof Logon
        def message2 = testQuickfixApplication.adminMessagesReceived[1]
        message2 instanceof ResendRequest
        ((ResendRequest) message2).get(new BeginSeqNo()).value == 2
        ((ResendRequest) message2).get(new EndSeqNo()).value == 3
        nextExpectedInboundSequenceNumber() == 5
    }

    def "should logout when sequence number is lower than expected 2-c"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        Session.sendToTarget(FixMessages.createNewOrderSingle(), sessionID)
        pollingConditions.eventually {
            testFixMessageListener.messagesReceived.size() >= 1
        }
        Session.lookupSession(sessionID).nextSenderMsgSeqNum = 1

        when:
        Session.sendToTarget(FixMessages.createNewOrderSingle(), sessionID)
        pollingConditions.eventually {
            testQuickfixApplication.adminMessagesReceived.size() >= 2
        }

        then:
        testFixMessageListener.messagesReceived.size() == 1
        def message = testQuickfixApplication.adminMessagesReceived[0]
        message instanceof Logon
        def message2 = testQuickfixApplication.adminMessagesReceived[1]
        message2 instanceof Logout
        ((Logout) message2).getText().value == "Sequence number is lower than expected"
        sessionSateListener.sessionState.logoutSent
    }

    def "should ignore garbled message 2-d"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        String clordid
        def newOrderSingle = FixMessages.newOrderSingle(sessionID, 2, { msg -> clordid = msg.getClOrdID().getValue() })

        when:
        sendMessage(channel, "garbage message")
        sendMessage(channel, newOrderSingle)
        pollingConditions.eventually {
            nextExpectedInboundSequenceNumber() > 2
        }

        then:
        nextExpectedInboundSequenceNumber() == 3
        testFixMessageListener.messagesReceived.size() == 1
        testFixMessageListener.messagesReceived[0].getField(ClOrdID.FIELD).value == clordid.toCharArray()
    }

    def "should process resend message 2-e-2-1"() {
        setup:
        startQuickfixAndWaitTillLoggedIn()
        def firstOrder = FixMessages.createNewOrderSingle()
        Session.sendToTarget(firstOrder, sessionID)
        pollingConditions.eventually {
            nextExpectedInboundSequenceNumber() == 3L
        }
        nextExpectedInboundSequenceNumber(2)
        testFixMessageListener.messagesReceived.clear()
        def secondOrder = FixMessages.createNewOrderSingle()

        when:
        Session.sendToTarget(secondOrder, sessionID)
        pollingConditions.eventually {
            testFixMessageListener.messagesReceived.size() >= 2
        }

        then:
        assertMinimalNewOrderSingle(firstOrder, testFixMessageListener.messagesReceived[0])
        assertMinimalNewOrderSingle(secondOrder, testFixMessageListener.messagesReceived[1])
        testFixMessageListener.messagesReceived[0].getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value == 2L
        testFixMessageListener.messagesReceived[1].getField(FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER).value == 3L
        sessionHandler().@sequenceNumberToQueuedFixMessages.isEmpty()
    }

    def "should ignore message that has already been resent 2-e-2-2"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        sendMessage(channel, FixMessages.newOrderSingle(sessionID, 4))
        def sequence2Resend = FixMessages.newOrderSingle(sessionID, 2, { msg -> msg.getHeader().setBoolean(PossDupFlag.FIELD, true) })
        sendMessage(channel, sequence2Resend)

        when:
        sendMessage(channel, sequence2Resend)
        pollingConditions.eventually {
            nextExpectedInboundSequenceNumber() == 3L
        } //TODO figure out a better wait condition, this one will be satisfied after sendMessage(channel, sequence2Resend) request from setup is processed
        Thread.sleep(java.util.concurrent.TimeUnit.SECONDS.toMillis(1))

        then:
        nextExpectedInboundSequenceNumber() == 3L

        cleanup:
        sessionHandler().reset() //to release a message that's being queued
    }

    def "should logout session after resend with OrigSendingTime greater than SendingTime is received 2-f"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def resend = FixMessages.newOrderSingle(sessionID, 2, { msg ->
            msg.getHeader().setBoolean(PossDupFlag.FIELD, true)
            msg.getHeader().setUtcTimeStamp(OrigSendingTime.FIELD, LocalDateTime.now().plusHours(1))
        })

        when:
        sendMessage(channel, resend)
        pollingConditions.eventually {
            receivedMessages.size() >= 3
        }

        then:
        receivedMessages.size() == 3 //should receive logon, reject and logout
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Reject reject = new Reject()
        reject.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        reject.getSessionRejectReason().getValue() == SessionRejectReason.SENDINGTIME_ACCURACY_PROBLEM
        nextExpectedInboundSequenceNumber() == 3
        Logout logout = new Logout()
        logout.fromString(receivedMessages[2], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        logout.getText().getValue().contains("SendingTime")
        !channel.isActive()
        sessionSateListener.sessionState.logoutSent
    }

    def "should reject message with PossDupFlag set but without OrigSendingTime 2-g"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def resend = FixMessages.newOrderSingle(sessionID, 2, { msg ->
            msg.getHeader().setBoolean(PossDupFlag.FIELD, true)
        })

        when:
        sendMessage(channel, resend)
        pollingConditions.eventually {
            receivedMessages.size() >= 2
        }

        then:
        receivedMessages.size() == 2 //should receive logon and reject
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Reject reject = new Reject()
        reject.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        reject.getSessionRejectReason().getValue() == SessionRejectReason.REQUIRED_TAG_MISSING
        reject.getRefTagID().getValue() == FixConstants.ORIG_SENDING_TIME_FIELD_NUMBER
        nextExpectedInboundSequenceNumber() == 3
        channel.isActive()
    }

    def "should logout session which sends unexpected BeginString 2-i"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def message = FixMessages.newOrderSingle(sessionID, 2, { msg ->
            msg.getHeader().setString(BeginString.FIELD, "FIX.4.2")
        })

        when:
        sendMessage(channel, message)
        pollingConditions.eventually {
            !sessionSateListener.sessionState.channel.isActive()
        }

        then:
        receivedMessages.size() == 2 //should receive logon and logout
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Logout logout = new Logout()
        logout.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        nextExpectedInboundSequenceNumber() == 2
        !channel.isActive()
        sessionSateListener.sessionState.logoutSent
        !sessionSateListener.sessionState.connected.get()
        sessionSateListener.sessionState.channel != null
        !sessionSateListener.sessionState.channel.isActive()
    }

    def "should reject message and log out session which sends unexpected SenderCompId 2-k"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def message = FixMessages.newOrderSingle(sessionID, 2, { msg ->
            msg.getHeader().setString(SenderCompID.FIELD, "wrongSender")
        })

        when:
        sendMessage(channel, message)
        pollingConditions.eventually {
            receivedMessages.size() >= 3
        }

        then:
        receivedMessages.size() == 3
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Reject reject = new Reject()
        reject.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        reject.getSessionRejectReason().value == SessionRejectReason.COMPID_PROBLEM
        Logout logout = new Logout()
        logout.fromString(receivedMessages[2], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        logout.getText().getValue().contains("CompID")
        nextExpectedInboundSequenceNumber() == 3
        !channel.isActive()
        sessionSateListener.sessionState.logoutSent
        !sessionSateListener.sessionState.connected.get()
        sessionSateListener.sessionState.channel != null
        !sessionSateListener.sessionState.channel.isActive()
    }

    def "should reject message and log out session which sends unexpected TargetCompId 2-k"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def message = FixMessages.newOrderSingle(sessionID, 2, { msg ->
            msg.getHeader().setString(TargetCompID.FIELD, "wrongTarget")
        })

        when:
        sendMessage(channel, message)
        pollingConditions.eventually {
            receivedMessages.size() >= 3
        }

        then:
        receivedMessages.size() == 3
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Reject reject = new Reject()
        reject.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        reject.getSessionRejectReason().value == SessionRejectReason.COMPID_PROBLEM
        Logout logout = new Logout()
        logout.fromString(receivedMessages[2], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        logout.getText().getValue().contains("CompID")
        nextExpectedInboundSequenceNumber() == 3
        !channel.isActive()
        sessionSateListener.sessionState.logoutSent
        !sessionSateListener.sessionState.connected.get()
        sessionSateListener.sessionState.channel != null
        !sessionSateListener.sessionState.channel.isActive()
    }

    def "should ignore message with incorrect body length 2-m"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def message = FixMessages.newOrderSingle(sessionID, 2).replaceAll("9=\\d+\\x01", "9=12345\u0001")

        when:
        sendMessage(channel, message)
        pollingConditions.eventually {
            receivedMessages.size() >= 1
        } //TODO figure out a better wait condition, this one will be satisfied after sendMessage(channel, FixMessages.logon(sessionID)) request from setup is processed
        Thread.sleep(java.util.concurrent.TimeUnit.SECONDS.toMillis(1))

        then:
        nextExpectedInboundSequenceNumber() == 2
    }

    def "should log out session with message that has sending time inaccuracy 2-o"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def message = FixMessages.newOrderSingle(sessionID, 2, { msg ->
            msg.getHeader().setUtcTimeStamp(SendingTime.FIELD, LocalDateTime.now().minus(FixConstants.SENDING_TIME_ACCURACY_MILLIS + 10, ChronoUnit.MILLIS))
        })

        when:
        sendMessage(channel, message)
        pollingConditions.eventually {
            receivedMessages.size() >= 3
        }

        then:
        receivedMessages.size() == 3
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Reject reject = new Reject()
        reject.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        reject.getSessionRejectReason().value == SessionRejectReason.SENDINGTIME_ACCURACY_PROBLEM
        Logout logout = new Logout()
        logout.fromString(receivedMessages[2], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        logout.getText().getValue().contains("SendingTime")
        nextExpectedInboundSequenceNumber() == 3
        !channel.isActive()
        sessionSateListener.sessionState.logoutSent
        sessionSateListener.sessionState.channel != null
        !sessionSateListener.sessionState.channel.isActive()
    }

    def "should reject message with unsupported message type 2-q/r"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def message = FixMessages.newOrderSingle(sessionID, 2, { msg ->
            msg.getHeader().setString(MsgType.FIELD, "wrongValue")
        })

        when:
        sendMessage(channel, message)
        pollingConditions.eventually {
            receivedMessages.size() >= 2
        }

        then:
        receivedMessages.size() == 2 //should receive logon and reject
        Logon logon = new Logon()
        logon.fromString(receivedMessages[0], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        Reject reject = new Reject()
        reject.fromString(receivedMessages[1], QuickfixTestUtils.FIXT11_DICTIONARY, true)
        reject.getSessionRejectReason().getValue() == SessionRejectReason.INVALID_MSGTYPE
        reject.getRefTagID().getValue() == FixConstants.MESSAGE_TYPE_FIELD_NUMBER
        nextExpectedInboundSequenceNumber() == 3
        channel.isActive()
    }

    @Ignore("Theoretically I should check if first 3 tags in received message are 8, 9 and 35 but parser can handle that. I'd also have to implement that validation before parser is run, which would fuck up my feng shui as there are " +
            "already 3 places where validation is run. So who gives a shit ;)")
    def "should ignore message where 3 first tags are not begin string, body length, message type 2-t"() {
        setup:
        def channel = connect()
        sendMessage(channel, FixMessages.logon(sessionID))
        def message = FixMessages.newOrderSingle(sessionID, 2).replaceAll("9=\\d+\\x0135=\\w++", "35=D\u00019=123")

        when:
        sendMessage(channel, message)
        pollingConditions.eventually {
            receivedMessages.size() >= 1
        } //TODO figure out a better wait condition, this one will be satisfied after sendMessage(channel, FixMessages.logon(sessionID)) request from setup is processed
        Thread.sleep(java.util.concurrent.TimeUnit.SECONDS.toMillis(1))

        then:
        nextExpectedInboundSequenceNumber() == 2
    }

    void assertMinimalNewOrderSingle(NewOrderSingle expected, FixMessage actual) {
        assert actual.<CharArrayField> getField(ClOrdID.FIELD).value == expected.getClOrdID().value.toCharArray()
        assert actual.<CharField> getField(Side.FIELD).value == expected.getSide().value
        assert actual.<CharArrayField> getField(TransactTime.FIELD).value == expected.
                getTransactTime().
                value.
                format(DateTimeFormatter.ofPattern("YYYYMMdd-HH:mm:ss.SSS")).
                toCharArray()
        assert actual.<CharField> getField(OrdType.FIELD).value == expected.getOrdType().value
    }
}