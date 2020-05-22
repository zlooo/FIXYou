package io.github.zlooo.fixyou.netty.utils

import io.netty.channel.*
import io.netty.util.concurrent.EventExecutorGroup
import org.assertj.core.api.Assertions

class TestPipeline implements ChannelPipeline {

    private List<Map.Entry<String, ChannelHandler>> handlers = new ArrayList<>()

    @Override
    ChannelPipeline addFirst(String name, ChannelHandler handler) {
        handlers.add(0, Assertions.entry(name, handler))
        return this
    }

    @Override
    ChannelPipeline addFirst(EventExecutorGroup group, String name, ChannelHandler handler) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline addLast(String name, ChannelHandler handler) {
        handlers.add(Assertions.entry(name, handler))
        return this
    }

    @Override
    ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler) {
        handlers.add(indexOf(baseName), Assertions.entry(name, handler))
        return this
    }

    @Override
    ChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name, ChannelHandler handler) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler) {
        handlers.add(indexOf(baseName) + 1, Assertions.entry(name, handler))
        return this
    }

    @Override
    ChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name, ChannelHandler handler) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline addFirst(ChannelHandler... handlers) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline addFirst(EventExecutorGroup group, ChannelHandler... handlers) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline addLast(ChannelHandler... handlers) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline addLast(EventExecutorGroup group, ChannelHandler... handlers) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline remove(ChannelHandler handler) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandler remove(String name) {
        throw new UnsupportedOperationException()
    }

    @Override
    def <T extends ChannelHandler> T remove(Class<T> handlerType) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandler removeFirst() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandler removeLast() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler) {
        def index = indexOf(oldName)
        def removedHandler = handlers.remove(index)
        handlers.add(index, Assertions.entry(newName, newHandler))
        return removedHandler.value
    }

    @Override
    def <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName, ChannelHandler newHandler) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandler first() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandlerContext firstContext() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandler last() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandlerContext lastContext() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandler get(String name) {
        def handlerIndex = indexOf(name)
        return handlerIndex >= 0 ? handlers[handlerIndex].value : null
    }

    @Override
    def <T extends ChannelHandler> T get(Class<T> handlerType) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandlerContext context(ChannelHandler handler) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandlerContext context(String name) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType) {
        throw new UnsupportedOperationException()
    }

    @Override
    Channel channel() {
        throw new UnsupportedOperationException()
    }

    @Override
    List<String> names() {
        handlers.collect { it.key }
    }

    @Override
    Map<String, ChannelHandler> toMap() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline fireChannelRegistered() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline fireChannelUnregistered() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline fireChannelActive() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline fireChannelInactive() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline fireExceptionCaught(Throwable cause) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline fireUserEventTriggered(Object event) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline fireChannelRead(Object msg) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline fireChannelReadComplete() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline fireChannelWritabilityChanged() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture bind(SocketAddress localAddress) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture connect(SocketAddress remoteAddress) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture disconnect() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture close() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture deregister() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture disconnect(ChannelPromise promise) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture close(ChannelPromise promise) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture deregister(ChannelPromise promise) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelOutboundInvoker read() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture write(Object msg) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture write(Object msg, ChannelPromise promise) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPipeline flush() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture writeAndFlush(Object msg) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPromise newPromise() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelProgressivePromise newProgressivePromise() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture newSucceededFuture() {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelFuture newFailedFuture(Throwable cause) {
        throw new UnsupportedOperationException()
    }

    @Override
    ChannelPromise voidPromise() {
        throw new UnsupportedOperationException()
    }

    @Override
    Iterator<Map.Entry<String, ChannelHandler>> iterator() {
        throw new UnsupportedOperationException()
    }

    int indexOf(String handlerName) {
        handlers.findIndexOf { it.key == handlerName }
    }
}
