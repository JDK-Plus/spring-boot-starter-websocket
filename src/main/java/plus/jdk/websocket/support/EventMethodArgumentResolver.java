package plus.jdk.websocket.support;

import io.netty.channel.Channel;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.core.MethodParameter;
import plus.jdk.websocket.annotations.OnWsEvent;

public class EventMethodArgumentResolver implements MethodArgumentResolver {

    private AbstractBeanFactory beanFactory;

    public EventMethodArgumentResolver(AbstractBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }


    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getMethod().isAnnotationPresent(OnWsEvent.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, Channel channel, Object object) {
        if (object==null) {
            return null;
        }
        TypeConverter typeConverter = beanFactory.getTypeConverter();
        return typeConverter.convertIfNecessary(object, parameter.getParameterType());
    }
}
