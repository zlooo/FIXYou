package io.github.zlooo.fixyou;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DefaultConfiguration { //TODO move to config module and make this configurable with default values

    public static final int FIX_MESSAGE_SUBSCRIBER_POOL_SIZE = 100;
    public static final int NUMBER_OF_REPETITIONS_IN_GROUP = 0;
    public static final int FIX_MESSAGE_LISTENER_INVOKER_DISRUPTOR_SIZE = 131072;
    public static final int FIX_MESSAGE_LISTENER_INVOKER_DISRUPTOR_TIMEOUT = 1;
    public static final int FIX_MESSAGE_POOL_SIZE = 131072;
    public static final int QUEUED_MESSAGES_MAP_SIZE = 10_000;
    public static final int DEFAULT_RECONNECT_INTERVAL = 30_000;
    public static final long DEFAULT_ENCRYPTION_METHOD = FixConstants.ENCRYPTION_METHOD_NONE;
    public static final long DEFAULT_HEARTBEAT_INTERVAL = 30;
    public static final String DEFAULT_ACCEPTOR_BIND_INTERFACE = "0.0.0.0";
    public static final int NESTED_REPEATING_GROUPS = 6;
    public static final int BYTE_BUF_COMPOSER_DEFAULT_COMPONENT_NUMBER = 10000;
    public static final int DEFAULT_OUT_MESSAGE_BUF_INIT_CAPACITY = 512;
    public static final int DEFAULT_MAX_FIELD_NUMBER = 200; //just a starting point, arrays will resize if needed
    public static final int INITIAL_FIELDS_IN_MSG_NUMBER = 20;
    public static final int INITIAL_REGION_ARRAY_SIZE = 5;
    public static final int REGION_POOL_SIZE = FIX_MESSAGE_POOL_SIZE;
    public static final short REGION_SIZE = 64 * 4; //cache line * 4
}
