package io.github.zlooo.fixyou.netty.test.framework

import quickfix.*
import quickfix.field.*
import quickfix.fixt11.MessageFactory

import java.time.LocalDateTime
import java.time.ZoneOffset

class QuickfixTestUtils {

    static final DataDictionary FIXT11_DICTIONARY = new DataDictionary(this.getResourceAsStream("/FIXT11.xml"))

    static Initiator setupInitiator(int connectPort, SessionID sessionID, Application application, String configFilePath) {
        def sessionSettings = new SessionSettings(configFilePath)
        sessionSettings.setLong("SocketConnectPort", connectPort)
        configureSession(sessionSettings, sessionID)
        return SocketInitiator.
                newBuilder().
                withApplication(application).
                withMessageFactory(new MessageFactory()).
                withSettings(sessionSettings).
                withMessageStoreFactory(new MemoryStoreFactory()).
                build()
    }

    private static void configureSession(SessionSettings sessionSettings, SessionID sessionID) {
        sessionSettings.setString(sessionID, "SenderCompID", sessionID.senderCompID)
        sessionSettings.setString(sessionID, "TargetCompID", sessionID.targetCompID)
        if (!sessionID.sessionQualifier?.isBlank()) {
            sessionSettings.setString(sessionID, "SessionQualifier", sessionID.sessionQualifier)
        }
    }

    static Acceptor setupAcceptor(int acceptPort, SessionID sessionID, Application application, String configFilePath) {
        def sessionSettings = new SessionSettings(configFilePath)
        sessionSettings.setLong("SocketAcceptPort", acceptPort)
        configureSession(sessionSettings, sessionID)
        return SocketAcceptor.
                newBuilder().
                withApplication(application).
                withMessageFactory(new MessageFactory()).
                withSettings(sessionSettings).
                withMessageStoreFactory(new MemoryStoreFactory()).
                build()
    }


    static putSessionIdInfo(SessionID sessionID, Message.Header header, boolean flipIDs) {
        if (flipIDs) {
            header.setString(SenderCompID.FIELD, sessionID.targetCompID)
            header.setString(TargetCompID.FIELD, sessionID.senderCompID)
        } else {
            header.setString(SenderCompID.FIELD, sessionID.senderCompID)
            header.setString(TargetCompID.FIELD, sessionID.targetCompID)
        }
        header.setString(BeginString.FIELD, sessionID.beginString)
    }

    static putStandardHeaderFields(Message.Header header, int sequenceNumber) {
        header.setInt(MsgSeqNum.FIELD, sequenceNumber)
        header.setUtcTimeStamp(SendingTime.FIELD, LocalDateTime.now(ZoneOffset.UTC))
    }
}
