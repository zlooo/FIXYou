package io.github.zlooo.fixyou.session;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.model.FixMessage;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Builder
@Getter
@FieldNameConstants
public class SessionConfig {

    private boolean persistent;
    private MessageStore<FixMessage> messageStore;
    private int port;
    private String host;
    @Builder.Default
    private boolean consolidateFlushes = true;
    @Builder.Default
    private long encryptMethod = DefaultConfiguration.DEFAULT_ENCRYPTION_METHOD;
    @Builder.Default
    private long heartbeatInterval = DefaultConfiguration.DEFAULT_HEARTBEAT_INTERVAL;
    @Singular
    private List<SessionStateListener> sessionStateListeners;
    @Builder.Default
    private ValidationConfig validationConfig = ValidationConfig.DEFAULT;
    @Builder.Default
    private StartStopConfig startStopConfig = StartStopConfig.INFINITE;
}
