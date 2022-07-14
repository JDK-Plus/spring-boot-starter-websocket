package plus.jdk.websocket.support;

import io.netty.channel.Channel;
import org.springframework.core.MethodParameter;
import plus.jdk.websocket.annotations.OnWsError;

import java.lang.reflect.Method;

public class ThrowableMethodArgumentResolver implements MethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Method method = parameter.getMethod();
        if(method == null) {
            return false;
        }
        return method.isAnnotationPresent(OnWsError.class) && Throwable.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel, Object object) throws Exception {
        if (object instanceof Throwable) {
            return object;
        }
        return null;
    }
}
