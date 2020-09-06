package io.github.zlooo.fixyou;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DefaultConfiguration { //TODO move to config module and make this configurable with default values

    public static final int FIX_MESSAGE_SUBSCRIBER_POOL_SIZE = 100;
    public static final int NUMBER_OF_REPETITIONS_IN_GROUP = 0;
    public static final int FIX_MESSAGE_LISTENER_INVOKER_DISRUPTOR_SIZE = 16384;
    public static final int FIX_MESSAGE_LISTENER_INVOKER_DISRUPTOR_TIMEOUT = 1;
    public static final int FIX_MESSAGE_READ_POOL_SIZE = 4096;
    public static final int FIX_MESSAGE_WRITE_POOL_SIZE = 4096;
    public static final int QUEUED_MESSAGES_MAP_SIZE = 10_000;
    public static final int DEFAULT_RECONNECT_INTERVAL = 30_000;
    public static final long DEFAULT_ENCRYPTION_METHOD = FixConstants.ENCRYPTION_METHOD_NONE;
    public static final long DEFAULT_HEARTBEAT_INTERVAL = 30;
    public static final String DEFAULT_ACCEPTOR_BIND_INTERFACE = "0.0.0.0";
    public static final int NESTED_REPEATING_GROUPS = 6;
    public static final int BYTE_BUF_COMPOSER_DEFAULT_COMPONENT_NUMBER = 10000;
    public static final int DEFAULT_OUT_MESSAGE_BUF_INIT_CAPACITY = 512;
}
