package io.github.zlooo.fixyou.commons

import spock.lang.Specification

class NamingThreadFactoryTest extends Specification {

    private Runnable runnable = Mock()

    def "should create new thread with apropriate name"() {
        setup:
        NamingThreadFactory threadFactory = new NamingThreadFactory("prefix", daemon)

        when:
        def thread = threadFactory.newThread(runnable)

        then:
        thread.getName() == "prefix-1"
        thread.isDaemon() == (daemon != null ? daemon : false)

        where:
        daemon | _
        true   | _
        false  | _
        null   | _
    }
}
