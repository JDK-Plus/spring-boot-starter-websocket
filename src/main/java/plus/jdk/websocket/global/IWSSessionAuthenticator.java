package plus.jdk.websocket.global;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import plus.jdk.websocket.model.IWsSession;

public interface IWSSessionAuthenticator<T extends IWsSession<?>> {

    T authenticate(Channel channel, FullHttpRequest req, String path) throws Exception;

    default void onSessionDestroy(IWsSession<T> session) {};
}
