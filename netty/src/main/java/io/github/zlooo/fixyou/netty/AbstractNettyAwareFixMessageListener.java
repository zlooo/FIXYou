package io.github.zlooo.fixyou.netty;

import io.github.zlooo.fixyou.fix.commons.FixMessageListener;
import io.netty.channel.Channel;
import lombok.Data;

@Data
public abstract class AbstractNettyAwareFixMessageListener implements FixMessageListener {

    private Channel channel;
}
