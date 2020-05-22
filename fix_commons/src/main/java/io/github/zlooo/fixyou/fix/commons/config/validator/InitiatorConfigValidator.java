package io.github.zlooo.fixyou.fix.commons.config.validator;

import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.session.SessionConfig;

import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

@Singleton
class InitiatorConfigValidator implements ConfigValidator {

    @Override
    public Set<String> validateConfig(FIXYouConfiguration fixYouConfiguration) {
        final Set<String> errorMessages = new HashSet<>();
        if (fixYouConfiguration.getReconnectIntervalMillis() <= 0) {
            errorMessages.add("Reconnect interval must be grater than 0");
        }
        return errorMessages;
    }

    @Override
    public Set<String> validateSessionConfig(SessionConfig sessionConfig) {
        final Set<String> errorMessages = new HashSet<>();
        Validations.checkPort(errorMessages, sessionConfig.getPort());
        Validations.positive(errorMessages, sessionConfig.getHeartbeatInterval(), SessionConfig.Fields.heartbeatInterval);
        Validations.checkEncryptMethod(errorMessages, sessionConfig.getEncryptMethod());
        if (sessionConfig.getHost() == null || sessionConfig.getHost().isEmpty()) {
            errorMessages.add("No host provided, need to know where to connect");
        } else if (new InetSocketAddress(sessionConfig.getHost(), sessionConfig.getPort()).isUnresolved()) {
            errorMessages.add("Invalid host provided");
        }
        Validations.checkPersistence(errorMessages, sessionConfig);
        return errorMessages;
    }
}
