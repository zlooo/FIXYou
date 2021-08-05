package io.github.zlooo.fixyou.fix.commons.config.validator

class Messages {

    static String invalidPort(int port) {
        "Invalid port provided, expecting value between (0, " + Validations.MAX_PORT_VALUE + "> but got " + port + " instead"
    }

    static String noBindInterface() {
        "No acceptor bind interface provided, need to be a valid interface address"
    }

    static String invalidBindInterface() {
        "Invalid acceptor bind interface provided"
    }

    static String noPersistence() {
        "Session is marked as persistent, yet no message store is provided"
    }

    static String positive(String fieldName) {
        fieldName + " should be a positive value"
    }

    static String noReconnectInterval() {
        "Reconnect interval must be grater than 0"
    }

    static String noHost() {
        "No host provided, need to know where to connect"
    }

    static String invalidHost() {
        "Invalid host provided"
    }

    static String encryptionNotSupported() {
        "Encryption is not supported yet"
    }

    static String noSslConfig() {
        "SSL configuration cannot be null when encryption is turned on"
    }

    static String noCertChainFile() {
        "Certificate chain file cannot be empty"
    }

    static String noPrivateKeyFile() {
        "Private key file cannot be empty"
    }
}
