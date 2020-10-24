package io.github.zlooo.fixyou.netty.test.framework

import io.github.zlooo.fixyou.FixConstants
import io.github.zlooo.fixyou.parser.model.FixMessage
import quickfix.SessionID
import quickfix.field.*
import quickfix.fix50sp2.NewOrderSingle
import quickfix.fixt11.Heartbeat
import quickfix.fixt11.Logon
import quickfix.fixt11.Logout
import quickfix.fixt11.TestRequest

import java.time.Instant
import java.time.LocalDateTime
import java.util.function.Consumer

class FixMessages {

    static String logon(SessionID sessionID, int sequenceNumber = 1, int heartbeatInterval = 30, Boolean resetSequenceNumber = null) {
        Logon logon = new Logon()
        QuickfixTestUtils.putSessionIdInfo(sessionID, logon.getHeader(), false)
        QuickfixTestUtils.putStandardHeaderFields(logon.getHeader(), sequenceNumber)
        logon.set(new EncryptMethod(EncryptMethod.NONE_OTHER))
        logon.set(new HeartBtInt(heartbeatInterval))
        logon.set(new DefaultApplVerID("7"))
        if (resetSequenceNumber != null) {
            logon.set(new ResetSeqNumFlag(resetSequenceNumber))
        }
        return logon.toString()
    }

    static String logout(SessionID sessionID, int sequenceNumber = 2, String text = null) {
        Logout logout = new Logout()
        QuickfixTestUtils.putSessionIdInfo(sessionID, logout.getHeader(), false)
        QuickfixTestUtils.putStandardHeaderFields(logout.getHeader(), sequenceNumber)
        if (text != null) {
            logout.set(new Text())
        }
        return logout.toString()
    }

    static String newOrderSingle(SessionID sessionID, int sequenceNumber, Consumer<NewOrderSingle> messageCustomizer = {}) {
        def newOrderSingle = createNewOrderSingle()
        QuickfixTestUtils.putSessionIdInfo(sessionID, newOrderSingle.getHeader(), false)
        QuickfixTestUtils.putStandardHeaderFields(newOrderSingle.getHeader(), sequenceNumber)
        messageCustomizer.accept(newOrderSingle)
        return newOrderSingle.toString()
    }

    static String testRequest(SessionID sessionID, String testRequestId, int sequenceNumber = 2) {
        def testRequest = new TestRequest()
        QuickfixTestUtils.putSessionIdInfo(sessionID, testRequest.getHeader(), false)
        QuickfixTestUtils.putStandardHeaderFields(testRequest.getHeader(), sequenceNumber)
        testRequest.set(new TestReqID(testRequestId))
        return testRequest
    }

    static String heartbeat(SessionID sessionID, String testRequestId, int sequenceNumber = 2) {
        def heartbeat = new Heartbeat()
        QuickfixTestUtils.putSessionIdInfo(sessionID, heartbeat.getHeader(), false)
        QuickfixTestUtils.putStandardHeaderFields(heartbeat.getHeader(), sequenceNumber)
        if (testRequestId != null) {
            heartbeat.set(new TestReqID(testRequestId))
        }
        return heartbeat
    }

    static NewOrderSingle createNewOrderSingle() {
        NewOrderSingle newOrderSingle = new NewOrderSingle()
        newOrderSingle.set(new ClOrdID(UUID.randomUUID().toString()))
        newOrderSingle.set(new Side(Side.BUY))
        newOrderSingle.set(new TransactTime(LocalDateTime.now()))
        newOrderSingle.set(new OrdType(OrdType.MARKET))
        return newOrderSingle
    }

    static Consumer<FixMessage> createFIXYouNewOrderSingle(UUID clordid) {
        return { fixMessage ->
            fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setCharSequenceValue(NewOrderSingle.MSGTYPE.toCharArray())
            fixMessage.getField(ClOrdID.FIELD).setCharSequenceValue(clordid.toString().toCharArray())
            fixMessage.getField(Side.FIELD).setCharValue(Side.BUY)
            fixMessage.getField(TransactTime.FIELD).setTimestampValue(Instant.now().toEpochMilli())
            fixMessage.getField(OrdType.FIELD).setCharValue(OrdType.MARKET)
        }
    }

    static Consumer<FixMessage> createFIXYouHeartbeat() {
        return { fixMessage ->
            fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).charSequenceValue = FixConstants.HEARTBEAT
            fixMessage.retain()
        }
    }
}
