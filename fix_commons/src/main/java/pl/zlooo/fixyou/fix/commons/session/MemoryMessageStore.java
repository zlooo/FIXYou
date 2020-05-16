package pl.zlooo.fixyou.fix.commons.session;

import lombok.extern.slf4j.Slf4j;
import org.agrona.collections.Hashing;
import org.agrona.collections.Long2ObjectHashMap;
import pl.zlooo.fixyou.DefaultConfiguration;
import pl.zlooo.fixyou.commons.pool.AbstractPoolableObject;
import pl.zlooo.fixyou.fix.commons.utils.FixMessageUtils;
import pl.zlooo.fixyou.parser.model.FixMessage;
import pl.zlooo.fixyou.session.LongSubscriber;
import pl.zlooo.fixyou.session.MessageStore;
import pl.zlooo.fixyou.session.SessionID;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Super simple <b>NOT THREAD SAFE</b> message store that uses Java heap to store messages. No capacity restrictions are implemented so this store can potentially cause {@link OutOfMemoryError}. Thus it should be used only for test purposes
 */
@Slf4j
public class MemoryMessageStore implements MessageStore<FixMessage> {

    private static final Function<SessionID, Long2ObjectHashMap<FixMessage>> MESSAGES_MAP_CREATOR = sessionID -> new Long2ObjectHashMap<>(DefaultConfiguration.QUEUED_MESSAGES_MAP_SIZE, Hashing.DEFAULT_LOAD_FACTOR);
    //TODO performance test it, maybe some other collection is better suited, especially since it's ordered
    private final Map<SessionID, Long2ObjectHashMap<FixMessage>> sessionToMessagesMap = new HashMap<>();

    @Override
    public void storeMessage(SessionID sessionID, long sequenceNumber, FixMessage message) {
        message.retain();
        sessionToMessagesMap.computeIfAbsent(sessionID, MESSAGES_MAP_CREATOR).put(sequenceNumber, message);
    }

    @Override
    public void getMessages(SessionID sessionID, long from, long to, LongSubscriber<FixMessage> messageSubscriber) {
        final Long2ObjectHashMap<FixMessage> messages = sessionToMessagesMap.get(sessionID);
        try {
            messageSubscriber.onSubscribe();
            if (messages != null && !messages.isEmpty()) {
                final long actualTo = to > 0 ? to : messages.size() - 1;
                for (long i = from; i <= actualTo; i++) {
                    final FixMessage message = messages.get(i);
                    messageSubscriber.onNext(i, message != null ? message : FixMessageUtils.EMPTY_FAKE_MESSAGE);
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
        final Long2ObjectHashMap<FixMessage> messages = sessionToMessagesMap.remove(sessionID);
        messages.values().forEach(AbstractPoolableObject::release);
    }
}
