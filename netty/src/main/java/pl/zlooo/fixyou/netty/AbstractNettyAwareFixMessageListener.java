package pl.zlooo.fixyou.netty;

import io.netty.channel.Channel;
import lombok.Data;
import pl.zlooo.fixyou.fix.commons.FixMessageListener;

@Data
public abstract class AbstractNettyAwareFixMessageListener implements FixMessageListener {

    private Channel channel;
}
