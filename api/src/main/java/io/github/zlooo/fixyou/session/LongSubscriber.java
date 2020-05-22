package io.github.zlooo.fixyou.session;

public interface LongSubscriber<T> {

    void onSubscribe();

    void onNext(long key, T item);

    void onError(Throwable throwable);

    void onComplete();
}
