package plus.jdk.websocket.global;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import plus.jdk.websocket.model.WsSession;

public class DefaultSessionAuthenticator implements IWSSessionAuthenticator<WsSession> {
    @Override
    public WsSession authenticate(Channel channel, FullHttpRequest req, String path) throws Exception{
        return new WsSession(channel.id().asShortText(), channel);
    }
}
