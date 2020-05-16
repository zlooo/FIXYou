package pl.zlooo.fixyou.netty.test.framework

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.zlooo.fixyou.fix.commons.FixMessageListener
import pl.zlooo.fixyou.parser.model.AbstractField
import pl.zlooo.fixyou.parser.model.FixMessage
import pl.zlooo.fixyou.session.SessionID

class TestFixMessageListener implements FixMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFixMessageListener)
    List<FixMessage> messagesReceived = new ArrayList<>()

    @Override
    void onFixMessage(SessionID sessionID, FixMessage fixMessage) {
        FixMessage msg = new FixMessage(TestSpec.INSTANCE)
        copyTo(fixMessage, msg)
        messagesReceived << msg
        LOGGER.info("Session with id {} received a message", sessionID, msg)
    }

    private static void copyTo(FixMessage from, FixMessage to) {
        to.resetAllDataFields();
        for (final AbstractField field : from.fields) {
            if (field != null) {
                to.getField(field.number).fieldData.writeBytes(field.fieldData.readerIndex(0));
            }
        }
    }
}
