package pl.zlooo.fixyou.netty.utils

import io.netty.channel.ChannelHandlerContext
import io.netty.util.AttributeKey
import spock.lang.Ignore
import spock.lang.Specification

import java.util.stream.Collectors
import java.util.stream.Stream

class DelegatingChannelHandlerContextTest extends Specification {

    private ChannelHandlerContext delegate = Mock()
    private DelegatingChannelHandlerContext channelHandlerContext = new DelegatingChannelHandlerContext(delegate)

    def "should delegate no arg operation"() {
        when:
        method.invoke(channelHandlerContext)

        then:
        1 * delegate./${method.name}/()

        where:
        method << Stream.of(ChannelHandlerContext.methods).filter({ method -> method.parameterCount == 0 }).collect(Collectors.toList())
    }

    def "should delegate one args operation"() {
        when:
        method.invoke(channelHandlerContext, param)

        then:
        1 * delegate./${method.name}/(param)

        where:
        method << Stream.of(ChannelHandlerContext.methods).filter({ method -> method.parameterCount == 1 }).collect(Collectors.toList())
        param = createParam(method.parameterTypes[0])
    }

    def "should delegate two args operation"() {
        when:
        method.invoke(channelHandlerContext, param1, param2)

        then:
        1 * delegate./${method.name}/(param1, param2)

        where:
        method << Stream.of(ChannelHandlerContext.methods).filter({ method -> method.parameterCount == 2 }).collect(Collectors.toList())
        param1 = createParam(method.parameterTypes[0])
        param2 = createParam(method.parameterTypes[1])
    }

    def "should delegate three args operation"() {
        when:
        method.invoke(channelHandlerContext, param1, param2, param3)

        then:
        1 * delegate./${method.name}/(param1, param2, param3)

        where:
        method << Stream.of(ChannelHandlerContext.methods).filter({ method -> method.parameterCount == 3 }).collect(Collectors.toList())
        param1 = createParam(method.parameterTypes[0])
        param2 = createParam(method.parameterTypes[1])
        param3 = createParam(method.parameterTypes[2])
    }

    @Ignore
    def "should delegate multiple args operation"() {
        //TODO make this test work instead of writing one/two/three args versions
        when:
        method.invoke(channelHandlerContext, *params)

        then:
        1 * delegate./${method.name}/(*params)

        where:
        method << Stream.of(ChannelHandlerContext.methods).filter({ method -> method.parameterCount == 1 }).collect(Collectors.toList())
        params = createParams(method.parameterTypes)
    }

    private <T> T createParam(Class<T> paramType) {
        if (paramType == AttributeKey) {
            return AttributeKey.valueOf("test")
        } else {
            return Mock(paramType)
        }
    }

    private List<Object> createParams(Class<?>[] paramTypes) {
        def params = []
        for (Class paramType : paramTypes) {
            if (paramType == AttributeKey) {
                params << AttributeKey.valueOf("test")
            } else {
                params << Mock(paramType)
            }
        }
    }
}
