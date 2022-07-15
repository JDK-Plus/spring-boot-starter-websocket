package plus.jdk.websocket.model;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public interface IWsSession<T> {

    T getUserId();

    void setUserId(T userId);
    
    Channel getChannel();
    
    void setChannel(Channel channel);

    default void setSubProtocols(String subProtocols) {
        setAttribute("subprotocols", subProtocols);
    }

    default ChannelFuture sendText(String message) {
        return getChannel().writeAndFlush(new TextWebSocketFrame(message));
    }

    default ChannelFuture sendText(ByteBuf byteBuf) {
        return getChannel().writeAndFlush(new TextWebSocketFrame(byteBuf));
    }

    default ChannelFuture sendText(ByteBuffer byteBuffer) {
        ByteBuf buffer = getChannel().alloc().buffer(byteBuffer.remaining());
        buffer.writeBytes(byteBuffer);
        return getChannel().writeAndFlush(new TextWebSocketFrame(buffer));
    }

    default ChannelFuture sendText(TextWebSocketFrame textWebSocketFrame) {
        return getChannel().writeAndFlush(textWebSocketFrame);
    }

    default ChannelFuture sendBinary(byte[] bytes) {
        ByteBuf buffer = getChannel().alloc().buffer(bytes.length);
        return getChannel().writeAndFlush(new BinaryWebSocketFrame(buffer.writeBytes(bytes)));
    }

    default ChannelFuture sendBinary(ByteBuf byteBuf) {
        return getChannel().writeAndFlush(new BinaryWebSocketFrame(byteBuf));
    }

    default ChannelFuture sendBinary(ByteBuffer byteBuffer) {
        ByteBuf buffer = getChannel().alloc().buffer(byteBuffer.remaining());
        buffer.writeBytes(byteBuffer);
        return getChannel().writeAndFlush(new BinaryWebSocketFrame(buffer));
    }

    default ChannelFuture sendBinary(BinaryWebSocketFrame binaryWebSocketFrame) {
        return getChannel().writeAndFlush(binaryWebSocketFrame);
    }

    default <T> void setAttribute(String name, T value) {
        AttributeKey<T> sessionIdKey = AttributeKey.valueOf(name);
        getChannel().attr(sessionIdKey).set(value);
    }

    default <T> T getAttribute(String name) {
        AttributeKey<T> sessionIdKey = AttributeKey.valueOf(name);
        return getChannel().attr(sessionIdKey).get();
    }


    /**
     * Returns the globally unique identifier of this {@link Channel}.
     */
    default ChannelId id() {
        return getChannel().id();
    }

    /**
     * Returns the configuration of this channel.
     */
    default ChannelConfig config() {
        return getChannel().config();
    }

    /**
     * Returns {@code true} if the {@link Channel} is open and may get active later
     */
    default boolean isOpen() {
        return getChannel().isOpen() ;
    }

    /**
     * Returns {@code true} if the {@link Channel} is registered with an {@link EventLoop}.
     */
    default boolean isRegistered() {
        return getChannel().isRegistered();
    }

    /**
     * Return {@code true} if the {@link Channel} is active and so connected.
     */
    default boolean isActive() {
        return getChannel().isActive();
    }

    /**
     * Return the {@link ChannelMetadata} of the {@link Channel} which describe the nature of the {@link Channel}.
     */
    default ChannelMetadata metadata() {
        return getChannel().metadata();
    }

    /**
     * Returns the local address where this channel is bound to.  The returned
     * {@link SocketAddress} is supposed to be down-cast into more concrete
     * type such as {@link InetSocketAddress} to retrieve the detailed
     * information.
     *
     * @return the local address of this channel.
     * {@code null} if this channel is not bound.
     */
    default SocketAddress localAddress() {
        return getChannel().localAddress();
    }

    /**
     * Returns the remote address where this channel is connected to.  The
     * returned {@link SocketAddress} is supposed to be down-cast into more
     * concrete type such as {@link InetSocketAddress} to retrieve the detailed
     * information.
     *
     * @return the remote address of this channel.
     * {@code null} if this channel is not connected.
     * If this channel is not connected but it can receive messages
     * from arbitrary remote addresses (e.g. {@link DatagramChannel},
     * use {@link DatagramPacket#recipient()} to determine
     * the origination of the received message as this method will
     * return {@code null}.
     */
    default SocketAddress remoteAddress() {
        return getChannel().remoteAddress();
    }

    /**
     * Returns the {@link ChannelFuture} which will be notified when this
     * channel is closed.  This method always returns the same future instance.
     */
    default ChannelFuture closeFuture() {
        return getChannel().closeFuture();
    }

    /**
     * Returns {@code true} if and only if the I/O thread will perform the
     * requested write operation immediately.  Any write requests made when
     * this method returns {@code false} are queued until the I/O thread is
     * ready to process the queued write requests.
     */
    default boolean isWritable() {
        return getChannel().isWritable();
    }

    /**
     * Get how many bytes can be written until {@link #isWritable()} returns {@code false}.
     * This quantity will always be non-negative. If {@link #isWritable()} is {@code false} then 0.
     */
    default long bytesBeforeUnwritable() {
        return getChannel().bytesBeforeUnwritable();
    }

    /**
     * Get how many bytes must be drained from underlying buffers until {@link #isWritable()} returns {@code true}.
     * This quantity will always be non-negative. If {@link #isWritable()} is {@code true} then 0.
     */
    default long bytesBeforeWritable() {
        return getChannel().bytesBeforeWritable();
    }

    /**
     * Returns an <em>internal-use-only</em> object that provides unsafe operations.
     */
    default Channel.Unsafe unsafe() {
        return getChannel().unsafe();
    }

    /**
     * Return the assigned {@link ChannelPipeline}.
     */
    default ChannelPipeline pipeline() {
        return getChannel().pipeline();
    }

    /**
     * Return the assigned {@link ByteBufAllocator} which will be used to allocate {@link ByteBuf}s.
     */
    default ByteBufAllocator alloc() {
        return getChannel().alloc();
    }

    default Channel read() {
        return getChannel().read();
    }

    default Channel flush() {
        return getChannel().flush();
    }

    default ChannelFuture close() {
        return getChannel().close();
    }

    default ChannelFuture close(ChannelPromise promise) {
        return getChannel().close(promise);
    }


}
