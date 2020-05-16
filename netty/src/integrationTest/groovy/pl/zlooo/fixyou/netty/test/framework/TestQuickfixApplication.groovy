package pl.zlooo.fixyou.netty.test.framework

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import quickfix.*
import quickfix.field.MsgType

class TestQuickfixApplication implements Application {
    private final static Logger LOGGER = LoggerFactory.getLogger(TestQuickfixApplication)
    final Set<SessionID> loggedOnSessions = new HashSet<>()
    final List<Message> adminMessagesReceived = new ArrayList<>()
    final List<Message> messagesReceived = new ArrayList<>()
    private final stopSessionAfterDisconnect

    TestQuickfixApplication(boolean stopSessionAfterDisconnect = true) {
        this.stopSessionAfterDisconnect = stopSessionAfterDisconnect
    }

    @Override
    void onCreate(SessionID sessionId) {

    }

    @Override
    void onLogon(SessionID sessionId) {
        loggedOnSessions << sessionId
    }

    @Override
    void onLogout(SessionID sessionId) {
    }

    @Override
    void toAdmin(Message message, SessionID sessionId) {

    }

    @Override
    void fromAdmin(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        adminMessagesReceived << message
        LOGGER.info("Got admin message {}, current size is {}", message, adminMessagesReceived.size())
        if (message.getHeader().getString(MsgType.FIELD).equals(MsgType.LOGOUT) && stopSessionAfterDisconnect) {
            Session.lookupSession(sessionId).logout()
        }
    }

    @Override
    void toApp(Message message, SessionID sessionId) throws DoNotSend {

    }

    @Override
    void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        messagesReceived << message
        LOGGER.info("Got message {}, current size is {}", message, messagesReceived.size())
    }
}
