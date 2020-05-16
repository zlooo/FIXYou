package pl.zlooo.fixyou.netty.utils;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import pl.zlooo.fixyou.Resettable;

import java.net.SocketAddress;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class DelegatingChannelHandlerContext implements ChannelHandlerContext, Resettable {

    private ChannelHandlerContext delegate;

    @Override
    public void reset() {
        delegate = null;
    }

    @Override
    public Channel channel() {
        return delegate.channel();
    }

    @Override
    public EventExecutor executor() {
        return delegate.executor();
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public ChannelHandler handler() {
        return delegate.handler();
    }

    @Override
    public boolean isRemoved() {
        return delegate.isRemoved();
    }

    @Override
    public ChannelHandlerContext fireChannelRegistered() {
        return delegate.fireChannelRegistered();
    }

    @Override
    public ChannelHandlerContext fireChannelUnregistered() {
        return delegate.fireChannelUnregistered();
    }

    @Override
    public ChannelHandlerContext fireChannelActive() {
        return delegate.fireChannelActive();
    }

    @Override
    public ChannelHandlerContext fireChannelInactive() {
        return delegate.fireChannelInactive();
    }

    @Override
    public ChannelHandlerContext fireExceptionCaught(Throwable cause) {
        return delegate.fireExceptionCaught(cause);
    }

    @Override
    public ChannelHandlerContext fireUserEventTriggered(Object evt) {
        return delegate.fireUserEventTriggered(evt);
    }

    @Override
    public ChannelHandlerContext fireChannelRead(Object msg) {
        return delegate.fireChannelRead(msg);
    }

    @Override
    public ChannelHandlerContext fireChannelReadComplete() {
        return delegate.fireChannelReadComplete();
    }

    @Override
    public ChannelHandlerContext fireChannelWritabilityChanged() {
        return delegate.fireChannelWritabilityChanged();
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return delegate.bind(localAddress);
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return delegate.bind(localAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return delegate.connect(remoteAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return delegate.connect(remoteAddress, localAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return delegate.connect(remoteAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return delegate.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public ChannelFuture disconnect() {
        return delegate.disconnect();
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return delegate.disconnect(promise);
    }

    @Override
    public ChannelFuture close() {
        return delegate.close();
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return delegate.close(promise);
    }

    @Override
    public ChannelFuture deregister() {
        return delegate.deregister();
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return delegate.deregister(promise);
    }

    @Override
    public ChannelHandlerContext read() {
        return delegate.read();
    }

    @Override
    public ChannelFuture write(Object msg) {
        return delegate.write(msg);
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return delegate.write(msg, promise);
    }

    @Override
    public ChannelHandlerContext flush() {
        return delegate.flush();
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return delegate.writeAndFlush(msg, promise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return delegate.writeAndFlush(msg);
    }

    @Override
    public ChannelPromise newPromise() {
        return delegate.newPromise();
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return delegate.newProgressivePromise();
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return delegate.newSucceededFuture();
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return delegate.newFailedFuture(cause);
    }

    @Override
    public ChannelPromise voidPromise() {
        return delegate.voidPromise();
    }

    @Override
    public ChannelPipeline pipeline() {
        return delegate.pipeline();
    }

    @Override
    public ByteBufAllocator alloc() {
        return delegate.alloc();
    }

    @Override
    @Deprecated
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return delegate.attr(key);
    }

    @Override
    @Deprecated
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return delegate.hasAttr(key);
    }
}
