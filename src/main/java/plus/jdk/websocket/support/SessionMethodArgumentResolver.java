package plus.jdk.websocket.support;

import io.netty.channel.Channel;
import org.springframework.core.MethodParameter;
import plus.jdk.websocket.model.IWsSession;

import static plus.jdk.websocket.global.WebsocketDispatcher.SESSION_KEY;

public class SessionMethodArgumentResolver implements MethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return IWsSession.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel, Object object) throws Exception {
        return channel.attr(SESSION_KEY).get();
    }
}
