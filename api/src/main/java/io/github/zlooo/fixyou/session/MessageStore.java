package io.github.zlooo.fixyou.session;

import io.github.zlooo.fixyou.SingleSessionResettable;

public interface MessageStore<T> extends SingleSessionResettable {

    void storeMessage(SessionID sessionID, long sequenceNumber, T message);

    void getMessages(SessionID sessionID, long from, long to, LongSubscriber<T> messageSubscriber);
}
