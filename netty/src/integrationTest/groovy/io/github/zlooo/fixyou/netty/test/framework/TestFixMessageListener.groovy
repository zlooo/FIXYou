package io.github.zlooo.fixyou.netty.test.framework

import io.github.zlooo.fixyou.commons.memory.Region
import io.github.zlooo.fixyou.commons.pool.ObjectPool
import io.github.zlooo.fixyou.fix.commons.FixMessageListener
import io.github.zlooo.fixyou.model.FixMessage
import io.github.zlooo.fixyou.parser.model.OffHeapFixMessage
import io.github.zlooo.fixyou.session.SessionID
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class TestFixMessageListener implements FixMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFixMessageListener)
    final BlockingQueue<OffHeapFixMessage> messagesReceived = new ArrayBlockingQueue<>(10)
    private final ObjectPool<Region> regionPool

    TestFixMessageListener(ObjectPool<Region> regionPool){
        this.regionPool = regionPool
    }

    @Override
    void onFixMessage(SessionID sessionID, FixMessage fixMessage) {
        assert fixMessage.refCnt() >= 1
        OffHeapFixMessage msg = new OffHeapFixMessage(regionPool)
        msg.copyDataFrom(fixMessage as OffHeapFixMessage)
        messagesReceived << msg
        LOGGER.info("Session with id {} received a message {}", sessionID, msg)
    }
}
