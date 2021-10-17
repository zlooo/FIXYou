package io.github.zlooo.fixyou.fix.commons.config.validator;

import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.session.SessionConfig;

import javax.inject.Singleton;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

@Singleton
class AcceptorConfigValidator implements ConfigValidator {

    @Override
    public Set<String> validateConfig(FIXYouConfiguration fixYouConfiguration) {
        final Set<String> errorMessages = new HashSet<>();
        Validations.checkPort(errorMessages, fixYouConfiguration.getAcceptorListenPort());
        if (fixYouConfiguration.getAcceptorBindInterface() == null || fixYouConfiguration.getAcceptorBindInterface().isEmpty()) {
            errorMessages.add("No acceptor bind interface provided, need to be a valid interface address");
        } else if (new InetSocketAddress(fixYouConfiguration.getAcceptorBindInterface(), 0).isUnresolved()) {
            errorMessages.add("Invalid acceptor bind interface provided");
        }
        if (fixYouConfiguration.isSslEnabled()) {
            Validations.checkSsl(errorMessages, fixYouConfiguration.getSslConfiguration());
        }
        return errorMessages;
    }

    @Override
    public Set<String> validateSessionConfig(SessionConfig sessionConfig) {
        final Set<String> errorMessages = new HashSet<>();
        Validations.checkPersistence(errorMessages, sessionConfig);
        Validations.positive(errorMessages, sessionConfig.getHeartbeatInterval(), SessionConfig.Fields.heartbeatInterval);
        Validations.checkEncryptMethod(errorMessages, sessionConfig.getEncryptMethod());
        Validations.checkSessionStartStop(errorMessages, sessionConfig.getStartStopConfig());
        return errorMessages;
    }
}
