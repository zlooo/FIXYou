package io.github.zlooo.fixyou.commons;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamingThreadFactory implements ThreadFactory {
    private final AtomicInteger id = new AtomicInteger(0);
    private final String namePrefix;
    private final Boolean daemon;

    public NamingThreadFactory(String namePrefix) {
        this(namePrefix, null);
    }

    public NamingThreadFactory(String namePrefix, Boolean daemon) {
        this.namePrefix = namePrefix;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        final Thread thread = new Thread(runnable, namePrefix + '-' + id.incrementAndGet());
        if (daemon != null) {
            thread.setDaemon(daemon);
        }
        return thread;
    }
}
