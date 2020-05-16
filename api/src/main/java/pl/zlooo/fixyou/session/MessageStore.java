package pl.zlooo.fixyou.session;

import pl.zlooo.fixyou.SingleSessionResettable;

public interface MessageStore<T> extends SingleSessionResettable {

    void storeMessage(SessionID sessionID, long sequenceNumber, T message);

    void getMessages(SessionID sessionID, long from, long to, LongSubscriber<T> messageSubscriber);
}
