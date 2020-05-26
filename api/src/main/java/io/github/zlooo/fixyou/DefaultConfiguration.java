package io.github.zlooo.fixyou;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DefaultConfiguration { //TODO move to config module and make this configurable with default values

    public static final int FIX_MESSAGE_SUBSCRIBER_POOL_SIZE = 100;
    public static final int AVG_FIELDS_PER_MESSAGE = 50;
    public static final int FIELD_BUFFER_SIZE = 50;
    public static final int NUMBER_OF_REPETITIONS_IN_GROUP = 2;
    public static final int FIX_MESSAGE_POOL_SIZE = 10;
    public static final int QUEUED_MESSAGES_MAP_SIZE = 10_000;
    public static final int DEFAULT_RECONNECT_INTERVAL = 30_000;
    public static final long DEFAULT_ENCRYPTION_METHOD = FixConstants.ENCRYPTION_METHOD_NONE;
    public static final long DEFAULT_HEARTBEAT_INTERVAL = 30;
    public static final String DEFAULT_ACCEPTOR_BIND_INTERFACE = "0.0.0.0";
    public static final int NESTED_REPEATING_GROUPS = 6;
}
