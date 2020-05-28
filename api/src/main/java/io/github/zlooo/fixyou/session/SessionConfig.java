package io.github.zlooo.fixyou.session;

import io.github.zlooo.fixyou.DefaultConfiguration;
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
    private MessageStore messageStore; //TODO should be <FixMessage> but it's in parser module. Need to change that and reorganize class hierarchy or FixMessage and all dependant classes
    private int port;
    private String host;
    private boolean consolidateFlushes;
    private long encryptMethod = DefaultConfiguration.DEFAULT_ENCRYPTION_METHOD;
    private long heartbeatInterval = DefaultConfiguration.DEFAULT_HEARTBEAT_INTERVAL;
    private final List<SessionStateListener> sessionStateListeners = new ArrayList<>();
    private ValidationConfig validationConfig = ValidationConfig.DEFAULT;

    public SessionConfig addSessionStateListener(SessionStateListener sessionStateListener) {
        sessionStateListeners.add(sessionStateListener);
        return this;
    }
}
