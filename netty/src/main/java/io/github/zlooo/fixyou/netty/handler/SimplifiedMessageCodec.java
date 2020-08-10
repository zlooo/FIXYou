package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.ApplicationVersionID;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.netty.NettyHandlerAwareSessionState;
import io.github.zlooo.fixyou.netty.utils.ValueAddingByteProcessor;
import io.github.zlooo.fixyou.parser.FixFieldsTypes;
import io.github.zlooo.fixyou.parser.FixMessageParser;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
@ChannelHandler.Sharable
class SimplifiedMessageCodec extends AbstractMessageEncoder implements ChannelInboundHandler {

    private static final SimplifiedSpec SIMPLIFIED_SPEC = new SimplifiedSpec();
    private final FixMessageParser fixMessageParser = new FixMessageParser();
    private final ByteBufComposer byteBufComposer = new ByteBufComposer(1);

    @Inject
    SimplifiedMessageCodec() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            final ByteBuf in = (ByteBuf) msg;
            try {
                byteBufComposer.addByteBuf(in);
                fixMessageParser.setFixBytes(byteBufComposer);
                /**
                 * This handler should handle 1 message only anyway, first logon, so we can afford invocation of {@link FixMessage#FixMessage(FixSpec)}. After that it's removed
                 * from
                 * pipeline by {@link io.github.zlooo.fixyou.netty.handler.admin.LogonHandler#addRequiredChannelsToPipeline(ChannelHandlerContext, NettyHandlerAwareSessionState)}
                 */
                final FixMessage fixMessage = new NotPoolableFixMessage(SIMPLIFIED_SPEC);
                fixMessageParser.setFixMessage(fixMessage);
                fixMessageParser.parseFixMsgBytes();
                if (fixMessageParser.isDone()) {
                    ctx.fireChannelRead(fixMessageParser.getFixMessage());
                } else {
                    log.error("Incomplete logon message arrived, closing channel {}", ctx.channel());
                    /**
                     * I know this looks like extreme measure, but handling fragmentation for simple logon message seems like an overkill for me. It is handled, however, for all
                     * other
                     * messages in {@link MessageDecoder}
                     */
                    ctx.close();
                }
            } finally {
                fixMessageParser.setFixMessage(null);
                in.release();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    protected ValueAddingByteProcessor getValueAddingByteProcessor() {
        return new ValueAddingByteProcessor();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelUnregistered();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelWritabilityChanged();
    }

    /**
     * All we need are fields that are relevant for {@link io.github.zlooo.fixyou.netty.handler.admin.LogonHandler}. The basic idea is that this spec is just for logon, then once
     * logged in a proper spec is used that is bound to session itself
     */
    private static final class SimplifiedSpec implements FixSpec {

        private final int highestFieldNumber = ArrayUtils.max(getFieldsOrder());

        @Override
        public int[] getFieldsOrder() {
            return new int[]{FixConstants.BEGIN_STRING_FIELD_NUMBER, FixConstants.BODY_LENGTH_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER,
                    FixConstants.APPL_VERSION_ID_FIELD_NUMBER,
                    FixConstants.SENDER_COMP_ID_FIELD_NUMBER, FixConstants.TARGET_COMP_ID_FIELD_NUMBER, FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER,
                    FixConstants.SENDING_TIME_FIELD_NUMBER, FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER,
                    FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.TEXT_FIELD_NUMBER, FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER,
                    FixConstants.USERNAME_FIELD_NUMBER, FixConstants.PASSWORD_FIELD_NUMBER, FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER,
                    FixConstants.CHECK_SUM_FIELD_NUMBER};
        }

        @Override
        public FieldType[] getTypes() {
            return new FieldType[]{FixFieldsTypes.BEGIN_STRING, FixFieldsTypes.BODY_LENGTH, FixFieldsTypes.MESSAGE_TYPE, FixFieldsTypes.APPL_VERSION_ID,
                    FixFieldsTypes.SENDER_COMP_ID,
                    FixFieldsTypes.TARGET_COMP_ID, FixFieldsTypes.MESSAGE_SEQUENCE_NUMBER, FixFieldsTypes.SENDING_TIME, FixFieldsTypes.ENCRYPT_METHOD,
                    FixFieldsTypes.HEARTBEAT_INTERVAL, FixFieldsTypes.BEGIN_SEQUENCE_NUMBER, FixFieldsTypes.END_SEQUENCE_NUMBER, FixFieldsTypes.TEXT, FixFieldsTypes.RESET_SEQ_NUMBER_FLAG, FixFieldsTypes.USERNAME,
                    FixFieldsTypes.PASSWORD,
                    FixFieldsTypes.DEFAULT_APP_VERSION_ID,
                    FixFieldsTypes.CHECK_SUM};
        }

        @Nonnull
        @Override
        public char[][] getMessageTypes() {
            return new char[][]{FixConstants.LOGON};
        }

        @Override
        public int highestFieldNumber() {
            return highestFieldNumber;
        }

        @Override
        public ApplicationVersionID applicationVersionId() {
            throw new UnsupportedOperationException("Simplified spec does not define application version id");
        }

        @Nullable
        @Override
        public FieldNumberTypePair[] getChildPairSpec(int groupNumber) {
            return null;
        }
    }

}
