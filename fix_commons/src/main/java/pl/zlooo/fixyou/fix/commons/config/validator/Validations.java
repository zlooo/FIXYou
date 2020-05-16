package pl.zlooo.fixyou.fix.commons.config.validator;

import lombok.experimental.UtilityClass;
import pl.zlooo.fixyou.FixConstants;
import pl.zlooo.fixyou.session.SessionConfig;

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
}
