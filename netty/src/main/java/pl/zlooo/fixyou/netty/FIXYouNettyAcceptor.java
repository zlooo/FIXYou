package pl.zlooo.fixyou.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import pl.zlooo.fixyou.FIXYouConfiguration;
import pl.zlooo.fixyou.fix.commons.config.validator.ConfigValidator;
import pl.zlooo.fixyou.netty.handler.FixYouNettyComponent;

import java.util.concurrent.Future;

class FIXYouNettyAcceptor extends AbstractFIXYouNetty {

    private final ServerBootstrap serverBootstrap;

    FIXYouNettyAcceptor(FixYouNettyComponent fixYouNettyComponent, FIXYouConfiguration fixYouConfiguration, ConfigValidator configValidator) {
        super(fixYouNettyComponent, fixYouConfiguration, configValidator);
        this.serverBootstrap = new ServerBootstrap().channel(NioServerSocketChannel.class).childHandler(fixYouNettyComponent.channelInitializer()).group(eventLoopGroup);
    }

    @Override
    public Future<Void> start() {
        return serverBootstrap.bind(fixYouConfiguration.getAcceptorBindInterface(), fixYouConfiguration.getAcceptorListenPort());
    }
}
