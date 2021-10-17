package io.github.zlooo.fixyou.fix.commons.config.validator;

import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.session.SessionConfig;
import io.github.zlooo.fixyou.session.StartStopConfig;
import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
class Validations {

    private static final int MAX_PORT_VALUE = 0xFFFF;

    static void checkPersistence(Set<String> errorMessages, SessionConfig sessionConfig) {
        if (sessionConfig.isPersistent() && sessionConfig.getMessageStore() == null) {
            errorMessages.add("Session is marked as persistent, yet no message store is provided");
        }
    }

    static void checkPort(Set<String> errorMessages, int port) {
        if (port <= 0 || port > MAX_PORT_VALUE) {
            errorMessages.add("Invalid port provided, expecting value between (0, " + MAX_PORT_VALUE + "> but got " + port + " instead");
        }
    }

    static void positive(Set<String> errorMessages, long valueToCheck, String fieldName) {
        if (valueToCheck <= 0) {
            errorMessages.add(fieldName + " should be a positive value");
        }
    }

    static void checkEncryptMethod(Set<String> errorMessages, long encryptMethod) {
        if (encryptMethod != FixConstants.ENCRYPTION_METHOD_NONE) {
            errorMessages.add("Encryption is not supported yet");
        }
    }

    static void checkSsl(Set<String> errorMessages, FIXYouConfiguration.SSLConfiguration sslConfiguration) {
        if (sslConfiguration == null) {
            errorMessages.add("SSL configuration cannot be null when encryption is turned on");
        } else {
            if (sslConfiguration.getCertChainFilePath() == null || sslConfiguration.getCertChainFilePath().trim().isEmpty()) {
                errorMessages.add("Certificate chain file cannot be empty");
            }
            if (sslConfiguration.getPrivateKeyFilePath() == null || sslConfiguration.getPrivateKeyFilePath().trim().isEmpty()) {
                errorMessages.add("Private key file cannot be empty");
            }
        }
    }

    static void checkSessionStartStop(Set<String> errorMessages, StartStopConfig startStopConfig) {
        if (startStopConfig != StartStopConfig.INFINITE) {
            boolean timeSet = true;
            if (startStopConfig.getStartTime() == null || startStopConfig.getStopTime() == null) {
                errorMessages.add("Start and stop times are mandatory");
                timeSet = false;
            }
            if (startStopConfig.getStartDay() != null && startStopConfig.getStopDay() != null) {
                if (!timeSet) {
                    errorMessages.add("If start and stop days are set, times need to be set as well");
                }
            } else if (startStopConfig.getStartDay() == null && startStopConfig.getStopDay() == null) {
                //nothing to do that's fine
            } else {
                errorMessages.add("Invalid start/stop day configuration, either both or none should be present");
            }
        }
    }
}
