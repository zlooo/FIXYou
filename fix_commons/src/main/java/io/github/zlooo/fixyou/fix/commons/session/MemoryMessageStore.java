package io.github.zlooo.fixyou.fix.commons.session;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.fix.commons.utils.EmptyFixMessage;
import io.github.zlooo.fixyou.model.FixMessage;
import io.github.zlooo.fixyou.session.LongSubscriber;
import io.github.zlooo.fixyou.session.MessageStore;
import io.github.zlooo.fixyou.session.SessionID;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Super simple <b>NOT THREAD SAFE</b> message store that uses Java heap to store messages. No capacity restrictions are implemented so this store can potentially cause {@link OutOfMemoryError}. Thus it should be used only for test purposes
 */
@Slf4j
public class MemoryMessageStore implements MessageStore<FixMessage> {

    private static final Function<SessionID, LongObjectMap<FixMessage>> MESSAGES_MAP_CREATOR = sessionID -> new LongObjectHashMap<>(DefaultConfiguration.QUEUED_MESSAGES_MAP_SIZE);
    //TODO performance test it, maybe some other collection is better suited, especially since it's ordered
    private final Map<SessionID, LongObjectMap<FixMessage>> sessionToMessagesMap = new HashMap<>();

    @Override
    public void storeMessage(SessionID sessionID, long sequenceNumber, FixMessage message) {
        log.debug("Storing message for session id {}, sequence number {}, message {}", sessionID, sequenceNumber, message);
        ReferenceCountUtil.retain(message);
        sessionToMessagesMap.computeIfAbsent(sessionID, MESSAGES_MAP_CREATOR).put(sequenceNumber, message);
    }

    @Override
    public void getMessages(SessionID sessionID, long from, long to, LongSubscriber<FixMessage> messageSubscriber) {
        final LongObjectMap<FixMessage> messages = sessionToMessagesMap.get(sessionID);
        try {
            messageSubscriber.onSubscribe();
            if (messages != null && !messages.isEmpty()) {
                final long actualTo = to > 0 ? to : messages.size() - 1;
                for (long i = from; i <= actualTo; i++) {
                    final FixMessage message = messages.get(i);
                    messageSubscriber.onNext(i, message != null ? message : EmptyFixMessage.INSTANCE);
                }
            }
            messageSubscriber.onComplete();
        } catch (Exception e) {
            log.error("Exception occured while getting messages from store", e);
            messageSubscriber.onError(e);
        }
    }

    @Override
    public void reset(SessionID sessionID) {
        final LongObjectMap<FixMessage> messages = sessionToMessagesMap.remove(sessionID);
        messages.values().forEach(ReferenceCountUtil::release);
    }
}
