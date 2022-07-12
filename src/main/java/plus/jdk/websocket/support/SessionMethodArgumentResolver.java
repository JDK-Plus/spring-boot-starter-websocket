package plus.jdk.websocket.support;

import io.netty.channel.Channel;
import org.springframework.core.MethodParameter;
import plus.jdk.websocket.model.WsSession;

import static plus.jdk.websocket.WebsocketDispatcher.SESSION_KEY;

public class SessionMethodArgumentResolver implements MethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return WsSession.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel, Object object) throws Exception {
        WsSession session = channel.attr(SESSION_KEY).get();
        return session;
    }
}
