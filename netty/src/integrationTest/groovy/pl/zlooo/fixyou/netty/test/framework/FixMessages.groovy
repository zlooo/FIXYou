package pl.zlooo.fixyou.netty.test.framework

import pl.zlooo.fixyou.FixConstants
import pl.zlooo.fixyou.parser.model.CharArrayField
import pl.zlooo.fixyou.parser.model.CharField
import pl.zlooo.fixyou.parser.model.FixMessage
import quickfix.SessionID
import quickfix.field.*
import quickfix.fix50sp2.NewOrderSingle
import quickfix.fixt11.Heartbeat
import quickfix.fixt11.Logon
import quickfix.fixt11.Logout
import quickfix.fixt11.TestRequest

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
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
            fixMessage.<CharArrayField> getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).setValue(NewOrderSingle.MSGTYPE.toCharArray())
            fixMessage.<CharArrayField> getField(ClOrdID.FIELD).setValue(clordid.toString().toCharArray())
            fixMessage.<CharField> getField(Side.FIELD).setValue(Side.BUY)
            fixMessage.<CharArrayField> getField(TransactTime.FIELD).setValue(FixConstants.UTC_TIMESTAMP_FORMATTER.format(OffsetDateTime.now(ZoneOffset.UTC)).toCharArray())
            fixMessage.<CharField> getField(OrdType.FIELD).setValue(OrdType.MARKET)
        }
    }

    static Consumer<FixMessage> createFIXYouHeartbeat() {
        return { fixMessage ->
            fixMessage.getField(FixConstants.MESSAGE_TYPE_FIELD_NUMBER).value = FixConstants.HEARTBEAT
            fixMessage.retain()
        }
    }
}
