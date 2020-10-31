package io.github.zlooo.fixyou.netty

import io.github.zlooo.fixyou.Engine
import io.github.zlooo.fixyou.FIXYouConfiguration
import io.github.zlooo.fixyou.FIXYouException
import io.github.zlooo.fixyou.fix.commons.FixMessageListener
import io.github.zlooo.fixyou.parser.model.FixMessage
import io.github.zlooo.fixyou.session.SessionID
import spock.lang.Specification

class FIXYouNettyTest extends Specification {

    private Engine engine = FIXYouNetty.create(FIXYouConfiguration.builder().fixMessageListenerInvokerDisruptorSize(2).fixMessagePoolSize(2).acceptorListenPort(123).build(), new TestFixMessageListener())

    def "should not log out session that's not started"() {
        when:
        FIXYouNetty.logoutSession(engine, new SessionID("beginString".chars, 11, "sender".chars, 6, "target".chars, 6)).get()

        then:
        thrown(FIXYouException)
    }

    def "should get fix message object pool"() {
        expect:
        FIXYouNetty.fixMessagePool(engine) != null
    }

    private final static class TestFixMessageListener implements FixMessageListener {

        @Override
        void onFixMessage(SessionID sessionID, FixMessage fixMessage) {

        }
    }
}
