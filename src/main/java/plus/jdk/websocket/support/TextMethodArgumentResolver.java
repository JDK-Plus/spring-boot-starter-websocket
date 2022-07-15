package plus.jdk.websocket.support;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.core.MethodParameter;
import plus.jdk.websocket.annotations.OnWsMessage;

import java.lang.reflect.Method;

public class TextMethodArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Method method = parameter.getMethod();
        if(method == null) {
            return false;
        }
        return method.isAnnotationPresent(OnWsMessage.class) && String.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel, Object object) throws Exception {
        TextWebSocketFrame textFrame = (TextWebSocketFrame) object;
        return textFrame.text();
    }
}
