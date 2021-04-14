package io.github.zlooo.fixyou.netty.handler;

import dagger.Lazy;
import io.github.zlooo.fixyou.FIXYouConfiguration;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;

import javax.inject.Inject;
import javax.inject.Singleton;

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
        if (fixYouConfiguration.isAddLoggingHandler()) {
            pipeline.addFirst(LOGGING_HANDLER);
        }
        pipeline.addLast(Handlers.GENERIC_DECODER.getName(), simplifiedMessageDecoder)
                .addLast(Handlers.MESSAGE_ENCODER.getName(), fixYouConfiguration.isFixSpecOrderedFields() ? fixSpecOrderedMessageEncoder.get() : unorderedMessageEncoder.get())
                .addLast(Handlers.GENERIC.getName(), genericHandler)
                .addLast(Handlers.ADMIN_MESSAGES.getName(), adminMessagesHandler)
                .addLast(Handlers.LISTENER_INVOKER.getName(), fixMessageListenerInvokingHandler);
    }
}
