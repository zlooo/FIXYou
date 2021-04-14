package io.github.zlooo.fixyou.netty.test.framework


import io.github.zlooo.fixyou.fix.commons.FixMessageListener
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage
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
        assert fixMessage.refCnt() >= 1
        FixMessage msg = new NotPoolableFixMessage()
        msg.copyDataFrom(fixMessage)
        messagesReceived << msg
        LOGGER.info("Session with id {} received a message {}", sessionID, msg)
    }
}
