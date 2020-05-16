package pl.zlooo.fixyou.netty.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FIXYouChannelInitializer extends ChannelInitializer<NioSocketChannel> {

    @Inject
    protected GenericHandler genericHandler;
    @Inject
    protected AdminMessagesHandler adminMessagesHandler;
    @Inject
    protected SimplifiedMessageCodec simplifiedMessageCodec;
    @Inject
    protected FixMessageListenerInvokingHandler fixMessageListenerInvokingHandler;

    @Inject
    FIXYouChannelInitializer() {
    }

    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        ch.pipeline()
          .addLast(Handlers.GENERIC_DECODER.getName(), simplifiedMessageCodec)
          .addLast(Handlers.GENERIC.getName(), genericHandler)
          .addLast(Handlers.ADMIN_MESSAGES.getName(), adminMessagesHandler)
          .addLast(Handlers.LISTENER_INVOKER.getName(), fixMessageListenerInvokingHandler);
    }
}
