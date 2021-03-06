package io.github.zlooo.fixyou.netty.test.framework

import io.github.zlooo.fixyou.commons.ByteBufComposer
import io.github.zlooo.fixyou.fix.commons.FixMessageListener
import io.github.zlooo.fixyou.parser.model.Field
import io.github.zlooo.fixyou.parser.model.FieldCodec
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.SessionID
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class TestFixMessageListener implements FixMessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestFixMessageListener)
    private static final FieldCodec FIELD_CODEC = new FieldCodec()
    BlockingQueue<FixMessage> messagesReceived = new ArrayBlockingQueue<>(10)

    @Override
    void onFixMessage(SessionID sessionID, FixMessage fixMessage) {
        assert fixMessage.refCnt() >= 1
        FixMessage msg = new FixMessage(FIELD_CODEC)
        copyTo(fixMessage, msg)
        messagesReceived << msg
        LOGGER.info("Session with id {} received a message {}", sessionID, msg)
    }

    private static void copyTo(FixMessage from, FixMessage to) {
        to.resetAllDataFieldsAndReleaseByteSource()
        to.messageByteSource = copy(from.messageByteSource)
        for (final Field field : from.allFields) {
            if (field != null && field.isValueSet()) {
                to.getField(field.number).setIndexes(field.getStartIndex(), field.getEndIndex())
            }
        }
    }

    private static ByteBufComposer copy(ByteBufComposer composer) {
        final ByteBufComposer copy = new ByteBufComposer(composer.components.length);
        copy.readerIndex = composer.readerIndex;
        copy.storedStartIndex = composer.storedStartIndex;
        copy.storedEndIndex = composer.storedEndIndex;
        copy.arrayIndex = composer.arrayIndex;
        for (int i = 0; i < composer.components.length; i++) {
            def component = composer.components[i];
            def componentCopy = new ByteBufComposer.Component();
            componentCopy.startIndex = component.startIndex;
            componentCopy.endIndex = component.endIndex;
            componentCopy.offset = component.offset
            if (componentCopy.startIndex != ByteBufComposer.INITIAL_VALUE) {
                componentCopy.buffer = component.buffer;
                componentCopy.buffer.retain();
            }
            copy.components[i] = componentCopy;
        }
        return copy;
    }
}
