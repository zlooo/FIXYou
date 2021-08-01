package io.github.zlooo.fixyou;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FIXYouConfiguration {

    @Builder.Default
    private int numberOfIOThreads = 1;
    @Builder.Default
    private int numberOfAppThreads = 1;
    @Builder.Default
    private boolean separateIoFromAppThread = true;
    @Builder.Default
    private int reconnectIntervalMillis = DefaultConfiguration.DEFAULT_RECONNECT_INTERVAL;
    @Builder.Default
    private String acceptorBindInterface = DefaultConfiguration.DEFAULT_ACCEPTOR_BIND_INTERFACE;
    private int acceptorListenPort;
    private boolean initiator;
    @Builder.Default
    private boolean addLoggingHandler = false;
    @Builder.Default
    private int fixMessagePoolSize = DefaultConfiguration.FIX_MESSAGE_POOL_SIZE;
    @Builder.Default
    private int fixMessageListenerInvokerDisruptorSize = DefaultConfiguration.FIX_MESSAGE_LISTENER_INVOKER_DISRUPTOR_SIZE;
    @Builder.Default
    private int regionPoolSize = DefaultConfiguration.REGION_POOL_SIZE;
    @Builder.Default
    private short regionSize = DefaultConfiguration.REGION_SIZE;
    private boolean fixSpecOrderedFields;
    private boolean sslEnabled;
    private SSLConfiguration sslConfiguration;

    @Builder
    @Getter
    public static class SSLConfiguration {
        private String trustChainFilePath;
        private String certChainFilePath;
        private String privateKeyFilePath;
        private String keyPassword;
    }
}
