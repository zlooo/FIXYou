package io.github.zlooo.fixyou.netty.handler;

import io.github.zlooo.fixyou.FixConstants;
import io.github.zlooo.fixyou.commons.ByteBufComposer;
import io.github.zlooo.fixyou.model.ApplicationVersionID;
import io.github.zlooo.fixyou.model.FieldType;
import io.github.zlooo.fixyou.model.FixSpec;
import io.github.zlooo.fixyou.parser.FixFieldsTypes;
import io.github.zlooo.fixyou.parser.FixMessageParser;
import io.github.zlooo.fixyou.parser.model.FixMessage;
import io.github.zlooo.fixyou.parser.model.NotPoolableFixMessage;
import io.github.zlooo.fixyou.utils.ArrayUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
@ChannelHandler.Sharable
class SimplifiedMessageDecoder extends ChannelInboundHandlerAdapter {

    private static final LogonOnlySpec LOGON_ONLY_SPEC = new LogonOnlySpec();

    @Inject
    SimplifiedMessageDecoder() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            final ByteBuf in = (ByteBuf) msg;
            final FixMessage fixMessage = new NotPoolableFixMessage();
            final ByteBufComposer byteBufComposer = new ByteBufComposer(1);
            try {
                /**
                 * This handler should handle 1 message only anyway, first logon, so we can afford couple of constructor invocations. After that it's removed from pipeline by
                 * {@link io.github.zlooo.fixyou.netty.handler.admin.LogonHandler#addRequiredChannelsToPipeline(ChannelHandlerContext, NettyHandlerAwareSessionState)}
                 */
                byteBufComposer.addByteBuf(in);
                final FixMessageParser fixMessageParser = new FixMessageParser(byteBufComposer, LOGON_ONLY_SPEC, fixMessage);
                fixMessageParser.startParsing();
                fixMessageParser.parseFixMsgBytes();
                if (fixMessageParser.isDone()) {
                    if (log.isTraceEnabled()) {
                        log.trace("Message after decoding {}", fixMessage.toString(true, LOGON_ONLY_SPEC));
                    }
                    ctx.fireChannelRead(fixMessage);
                } else {
                    log.error("Incomplete logon message arrived, closing channel {}", ctx.channel());
                    /**
                     * I know this looks like extreme measure, but handling fragmentation for simple logon message seems like an overkill for me. It is handled, however, for all
                     * other messages in {@link MessageDecoder}
                     */
                    ctx.close();
                }
            } finally {
                fixMessage.close();
                byteBufComposer.reset();
                in.release();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * All we need are fields that are relevant for {@link io.github.zlooo.fixyou.netty.handler.admin.LogonHandler}. The basic idea is that this spec is just for logon, then once
     * logged in a proper spec is used that is bound to session itself
     */
    private static final class LogonOnlySpec implements FixSpec {

        private static final int[] HEADER_FIELDS_ORDER =
                new int[]{FixConstants.BEGIN_STRING_FIELD_NUMBER, FixConstants.BODY_LENGTH_FIELD_NUMBER, FixConstants.MESSAGE_TYPE_FIELD_NUMBER, FixConstants.APPL_VERSION_ID_FIELD_NUMBER, FixConstants.SENDER_COMP_ID_FIELD_NUMBER,
                        FixConstants.TARGET_COMP_ID_FIELD_NUMBER, FixConstants.MESSAGE_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.SENDING_TIME_FIELD_NUMBER};
        private static final int[] BODY_FIELDS_ORDER =
                {FixConstants.ENCRYPT_METHOD_FIELD_NUMBER, FixConstants.HEARTBEAT_INTERVAL_FIELD_NUMBER, FixConstants.BEGIN_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.END_SEQUENCE_NUMBER_FIELD_NUMBER, FixConstants.TEXT_FIELD_NUMBER,
                        FixConstants.RESET_SEQUENCE_NUMBER_FLAG_FIELD_NUMBER, FixConstants.USERNAME_FIELD_NUMBER, FixConstants.PASSWORD_FIELD_NUMBER, FixConstants.DEFAULT_APP_VERSION_ID_FIELD_NUMBER, FixConstants.CHECK_SUM_FIELD_NUMBER};
        private static final FieldType[] HEADER_FIELD_TYPES =
                new FieldType[]{FixFieldsTypes.BEGIN_STRING, FixFieldsTypes.BODY_LENGTH, FixFieldsTypes.MESSAGE_TYPE, FixFieldsTypes.APPL_VERSION_ID, FixFieldsTypes.SENDER_COMP_ID, FixFieldsTypes.TARGET_COMP_ID,
                        FixFieldsTypes.MESSAGE_SEQUENCE_NUMBER, FixFieldsTypes.SENDING_TIME};
        private static final FieldType[] BODY_FIELD_TYPES =
                {FixFieldsTypes.ENCRYPT_METHOD, FixFieldsTypes.HEARTBEAT_INTERVAL, FixFieldsTypes.BEGIN_SEQUENCE_NUMBER, FixFieldsTypes.END_SEQUENCE_NUMBER, FixFieldsTypes.TEXT, FixFieldsTypes.RESET_SEQUENCE_NUMBER_FLAG,
                        FixFieldsTypes.USERNAME, FixFieldsTypes.PASSWORD, FixFieldsTypes.DEFAULT_APP_VERSION_ID, FixFieldsTypes.CHECK_SUM};
        private static final char[][] MESSAGE_TYPES = {FixConstants.LOGON};

        @Nonnull
        @Override
        public int[] getHeaderFieldsOrder() {
            return HEADER_FIELDS_ORDER;
        }

        @Nonnull
        @Override
        public FieldType[] getHeaderFieldTypes() {
            return HEADER_FIELD_TYPES;
        }

        @Nonnull
        @Override
        public int[] getBodyFieldsOrder() {
            return BODY_FIELDS_ORDER;
        }

        @Nonnull
        @Override
        public FieldType[] getBodyFieldTypes() {
            return BODY_FIELD_TYPES;
        }

        @Nonnull
        @Override
        public char[][] getMessageTypes() {
            return MESSAGE_TYPES;
        }

        @Nonnull
        @Override
        public ApplicationVersionID applicationVersionId() {
            throw new UnsupportedOperationException("Simplified spec does not define application version id");
        }

        @Nonnull
        @Override
        public FieldNumberType[] getRepeatingGroupFieldNumbers(int groupNumber) {
            return ArrayUtils.EMPTY_FIELD_NUMBER_TYPE;
        }
    }

}
