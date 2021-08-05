package io.github.zlooo.fixyou.netty.handler;

import dagger.Lazy;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import lombok.SneakyThrows;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.FileInputStream;

@Singleton
public class FIXYouChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    public static final AttributeKey<Integer> ORDINAL_NUMBER_KEY = AttributeKey.valueOf("ordinalNumber");
    private static final LoggingHandler LOGGING_HANDLER = new LoggingHandler();

    @Inject
    protected GenericHandler genericHandler;
    @Inject
    protected AdminMessagesHandler adminMessagesHandler;
    @Inject
    protected SimplifiedMessageDecoder simplifiedMessageDecoder;
    @Inject
    protected Lazy<FixSpecOrderedMessageEncoder> fixSpecOrderedMessageEncoder;
    @Inject
    protected Lazy<UnorderedMessageEncoder> unorderedMessageEncoder;
    @Inject
    @NamedHandler(Handlers.LISTENER_INVOKER)
    protected ChannelHandler fixMessageListenerInvokingHandler;
    private final FIXYouConfiguration fixYouConfiguration;

    @Inject
    FIXYouChannelInitializer(FIXYouConfiguration fixYouConfiguration) {
        this.fixYouConfiguration = fixYouConfiguration;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        ch.attr(ORDINAL_NUMBER_KEY).set(ch.hashCode() % fixYouConfiguration.getNumberOfAppThreads());
        final ChannelPipeline pipeline = ch.pipeline();
        configureSsl(fixYouConfiguration, pipeline);
        if (fixYouConfiguration.isAddLoggingHandler()) {
            pipeline.addFirst(LOGGING_HANDLER);
        }
        pipeline.addLast(Handlers.GENERIC_DECODER.getName(), simplifiedMessageDecoder)
                .addLast(Handlers.MESSAGE_ENCODER.getName(), fixYouConfiguration.isFixSpecOrderedFields() ? fixSpecOrderedMessageEncoder.get() : unorderedMessageEncoder.get())
                .addLast(Handlers.GENERIC.getName(), genericHandler)
                .addLast(Handlers.ADMIN_MESSAGES.getName(), adminMessagesHandler)
                .addLast(Handlers.LISTENER_INVOKER.getName(), fixMessageListenerInvokingHandler);
    }

    @SneakyThrows
    private static void configureSsl(FIXYouConfiguration fixYouConfiguration, ChannelPipeline pipeline) {
        if (fixYouConfiguration.isSslEnabled()) {
            final FIXYouConfiguration.SSLConfiguration sslConfiguration = fixYouConfiguration.getSslConfiguration();
            final SslContextBuilder sslContextBuilder;
            final String certChainFilePath = sslConfiguration.getCertChainFilePath();
            final String privateKeyFilePath = sslConfiguration.getPrivateKeyFilePath();
            if (!fixYouConfiguration.isInitiator()) {
                sslContextBuilder = SslContextBuilder.forServer(new FileInputStream(certChainFilePath), new FileInputStream(privateKeyFilePath), sslConfiguration.getKeyPassword());
            } else {
                sslContextBuilder = SslContextBuilder.forClient();
                if (privateKeyFilePath != null) {
                    sslContextBuilder.keyManager(certChainFilePath != null ? new FileInputStream(certChainFilePath) : null, new FileInputStream(privateKeyFilePath), sslConfiguration.getKeyPassword());
                }
            }
            if (sslConfiguration.getTrustChainFilePath() != null) {
                sslContextBuilder.trustManager(new FileInputStream(sslConfiguration.getTrustChainFilePath()));
            }
            pipeline.addFirst(new SslHandler(sslContextBuilder.build().newEngine(pipeline.channel().alloc())));
        }
    }
}
