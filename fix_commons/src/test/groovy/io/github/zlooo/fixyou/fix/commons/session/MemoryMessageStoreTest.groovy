package io.github.zlooo.fixyou.fix.commons.session

import io.github.zlooo.fixyou.fix.commons.TestSpec
import io.github.zlooo.fixyou.fix.commons.utils.FixMessageUtils
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.LongSubscriber
import io.github.zlooo.fixyou.session.SessionID
import org.assertj.core.api.Assertions
import spock.lang.Specification

class MemoryMessageStoreTest extends Specification {

    private SessionID sessionID = new SessionID([] as char[], 0, [] as char[], 0, [] as char[], 0)
    private MemoryMessageStore store = new MemoryMessageStore()

    def "should store message"() {
        setup:
        FixMessage msg = new FixMessage(TestSpec.INSTANCE)

        when:
        store.storeMessage(sessionID, 666, msg)

        then:
        msg.refCnt() == 1
        Assertions.assertThat(store.@sessionToMessagesMap).containsOnlyKeys(sessionID)
        Assertions.assertThat(store.@sessionToMessagesMap[sessionID]).containsOnly(Assertions.entry(666L, msg))
    }

    def "should load messages"() {
        setup:
        List<FixMessage> msgs = []
        for (i in 0..6) {
            msgs << new FixMessage(TestSpec.INSTANCE)
        }
        store.@sessionToMessagesMap.computeIfAbsent(sessionID, MemoryMessageStore.MESSAGES_MAP_CREATOR).putAll(msgs.indexed().collectEntries { key, value -> [key.toLong(), value] })
        def testSub = new TestLongSubscriber()

        when:
        store.getMessages(sessionID, 3, 5, testSub)

        then:
        testSub.subscribeCalled
        testSub.completeCalled
        testSub.error == null
        Assertions.assertThat(testSub.items).containsOnly(Assertions.entry(3L, msgs[3]), Assertions.entry(4L, msgs[4]), Assertions.entry(5L, msgs[5]))
    }

    def "should load messages even when excessive amount is requested"() {
        setup:
        List<FixMessage> msgs = []
        for (i in 0..4) {
            msgs << new FixMessage(TestSpec.INSTANCE)
        }
        store.@sessionToMessagesMap.computeIfAbsent(sessionID, MemoryMessageStore.MESSAGES_MAP_CREATOR).putAll(msgs.indexed().collectEntries { key, value -> [key.toLong(), value] })
        def testSub = new TestLongSubscriber()

        when:
        store.getMessages(sessionID, 3, 5, testSub)

        then:
        testSub.subscribeCalled
        testSub.completeCalled
        testSub.error == null
        Assertions.assertThat(testSub.items).containsOnly(Assertions.entry(3L, msgs[3]), Assertions.entry(4L, msgs[4]), Assertions.entry(5L, FixMessageUtils.EMPTY_FAKE_MESSAGE))
    }

    def "should load all messages subsequent to provided sequence number"() {
        setup:
        List<FixMessage> msgs = []
        for (i in 0..6) {
            msgs << new FixMessage(TestSpec.INSTANCE)
        }
        store.@sessionToMessagesMap.computeIfAbsent(sessionID, MemoryMessageStore.MESSAGES_MAP_CREATOR).putAll(msgs.indexed().collectEntries { key, value -> [key.toLong(), value] })
        def testSub = new TestLongSubscriber()

        when:
        store.getMessages(sessionID, 3, 0, testSub)

        then:
        testSub.subscribeCalled
        testSub.completeCalled
        testSub.error == null
        Assertions.assertThat(testSub.items).containsOnly(Assertions.entry(3L, msgs[3]), Assertions.entry(4L, msgs[4]), Assertions.entry(5L, msgs[5]), Assertions.entry(6L, msgs[6]))
    }

    def "should notify about exception when it happens during message processing"() {
        setup:
        List<FixMessage> msgs = []
        for (i in 0..6) {
            msgs << new FixMessage(TestSpec.INSTANCE)
        }
        store.@sessionToMessagesMap.computeIfAbsent(sessionID, MemoryMessageStore.MESSAGES_MAP_CREATOR).putAll(msgs.indexed().collectEntries { key, value -> [key.toLong(), value] })
        def testSub = new TestLongSubscriber()
        testSub.keyThatShouldThrowException = 4

        when:
        store.getMessages(sessionID, 3, 0, testSub)

        then:
        testSub.subscribeCalled
        !testSub.completeCalled
        testSub.error != null
        testSub.error instanceof RuntimeException
        testSub.error.getMessage() == "Test"
        Assertions.assertThat(testSub.items).containsOnly(Assertions.entry(3L, msgs[3]))
    }

    def "should release all for given session messages when store is reset"() {
        setup:
        FixMessage msg = new FixMessage(TestSpec.INSTANCE)
        store.storeMessage(sessionID, 666, msg)
        SessionID sessionID2 = new SessionID(['2'] as char[], 1, [] as char[], 0, [] as char[], 0)
        FixMessage msg2 = new FixMessage(TestSpec.INSTANCE)
        store.storeMessage(sessionID2, 666, msg2)

        when:
        store.reset(sessionID)

        then:
        msg.refCnt() == 0
        Assertions.assertThat(store.@sessionToMessagesMap).hasSize(1).containsOnlyKeys(sessionID2)
    }

    private static final class TestLongSubscriber implements LongSubscriber<FixMessage> {

        private boolean subscribeCalled = false
        private Map<Long, FixMessage> items = new HashMap<>()
        private Throwable error
        private boolean completeCalled = false
        private long keyThatShouldThrowException = -1

        @Override
        void onSubscribe() {
            subscribeCalled = true
        }

        @Override
        void onNext(long key, FixMessage item) {
            if (keyThatShouldThrowException == key) {
                throw new RuntimeException("Test")
            }
            items.put(key, item)
        }

        @Override
        void onError(Throwable throwable) {
            error = throwable
        }

        @Override
        void onComplete() {
            completeCalled = true
        }
    }
}
