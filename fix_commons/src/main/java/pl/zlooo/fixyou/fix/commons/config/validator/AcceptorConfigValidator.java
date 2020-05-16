package pl.zlooo.fixyou.fix.commons.config.validator;

import pl.zlooo.fixyou.FIXYouConfiguration;
import pl.zlooo.fixyou.session.SessionConfig;

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
        return errorMessages;
    }

    @Override
    public Set<String> validateSessionConfig(SessionConfig sessionConfig) {
        final Set<String> errorMessages = new HashSet<>();
        Validations.checkPersistence(errorMessages, sessionConfig);
        Validations.positive(errorMessages, sessionConfig.getHeartbeatInterval(), SessionConfig.Fields.heartbeatInterval);
        Validations.checkEncryptMethod(errorMessages, sessionConfig.getEncryptMethod());
        return errorMessages;
    }
}
