package plus.jdk.websocket.global;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import plus.jdk.websocket.model.WsSession;
import plus.jdk.websocket.properties.WebsocketProperties;

public class DefaultSessionAuthenticatorManager implements IWSSessionAuthenticatorManager<String, WsSession> {
    @Override
    public WsSession authenticate(Channel channel, FullHttpRequest req, String path, WebsocketProperties properties) throws Exception{
        return new WsSession(channel.id().asShortText(), channel);
    }
}
