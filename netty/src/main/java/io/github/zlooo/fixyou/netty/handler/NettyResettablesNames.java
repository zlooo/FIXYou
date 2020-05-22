package io.github.zlooo.fixyou.netty.handler;

import lombok.experimental.UtilityClass;

@UtilityClass
public class NettyResettablesNames {

    public static final String SESSION = Handlers.SESSION.getName();
    public static final String MESSAGE_ENCODER = Handlers.MESSAGE_ENCODER.getName();
    public static final String MESSAGE_DECODER = Handlers.MESSAGE_DECODER.getName();
    public static final String IDLE_STATE_HANDLER = Handlers.IDLE_STATE_HANDLER.getName();
    public static final String NOT_MOVING_FORWARD_ON_READ_AND_WRITE_CHANNEL_HANDLER_CONTEXT = "notMovingForwardOnReadAndWriteChannelHandlerContext";
    public static final String MESSAGE_STORE_HANDLER = Handlers.MESSAGE_STORE_HANDLER.getName();
    public static final String FLUSH_CONSOLIDATION_HANDLER = Handlers.FLUSH_CONSOLIDATION_HANDLER.getName();
}
