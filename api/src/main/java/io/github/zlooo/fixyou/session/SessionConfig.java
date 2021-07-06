package io.github.zlooo.fixyou.session;

import io.github.zlooo.fixyou.DefaultConfiguration;
import io.github.zlooo.fixyou.model.FixMessage;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@FieldNameConstants
public class SessionConfig {

    private boolean persistent;
    private MessageStore<FixMessage> messageStore;
    private int port;
    private String host;
    private boolean consolidateFlushes = true;
    private long encryptMethod = DefaultConfiguration.DEFAULT_ENCRYPTION_METHOD;
    private long heartbeatInterval = DefaultConfiguration.DEFAULT_HEARTBEAT_INTERVAL;
    private final List<SessionStateListener> sessionStateListeners = new ArrayList<>();
    private ValidationConfig validationConfig = ValidationConfig.DEFAULT;

    public SessionConfig addSessionStateListener(SessionStateListener sessionStateListener) {
        sessionStateListeners.add(sessionStateListener);
        return this;
    }
}
