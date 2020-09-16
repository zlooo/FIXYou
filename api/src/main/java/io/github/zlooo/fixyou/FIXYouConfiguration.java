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
}
