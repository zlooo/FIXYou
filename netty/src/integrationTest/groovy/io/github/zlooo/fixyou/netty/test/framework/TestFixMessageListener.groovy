package io.github.zlooo.fixyou.netty.test.framework

import io.github.zlooo.fixyou.fix.commons.FixMessageListener
import io.github.zlooo.fixyou.parser.model.AbstractField
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.SessionID
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class TestFixMessageListener implements FixMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFixMessageListener)
    BlockingQueue<FixMessage> messagesReceived = new ArrayBlockingQueue<>(10)

    @Override
    void onFixMessage(SessionID sessionID, FixMessage fixMessage) {
        FixMessage msg = new FixMessage(TestSpec.INSTANCE)
        copyTo(fixMessage, msg)
        messagesReceived << msg
        LOGGER.info("Session with id {} received a message {}", sessionID, msg)
    }

    private static void copyTo(FixMessage from, FixMessage to) {
        to.resetAllDataFields()
        to.messageByteSource = from.messageByteSource//.copy()
        for (final AbstractField field : from.fields) {
            if (field != null && field.isValueSet()) {
                to.getField(field.number).setIndexes(field.getStartIndex(), field.getEndIndex())
            }
        }
    }
}
