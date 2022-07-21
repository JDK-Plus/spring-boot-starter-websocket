package plus.jdk.websocket.global;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.Data;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import plus.jdk.websocket.common.WebsocketCommonException;
import plus.jdk.websocket.support.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import plus.jdk.websocket.support.RequestParamMapMethodArgumentResolver;

@Data
public class WebsocketMethodMapping {

    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 请求路径
     */
    private String path;

    /**
     * 由哪个类来处理请求
     */
    private Class<?> handlerClazz;

    /**
     * 处理当前请求的bean对象
     */
    private Object beanObject;


    /**
     * 用来处理连接打开时的方法
     */
    private Method onOpenMethod;

    /**
     * 用来处理收到消息时的方法
     */
    private Method onMessageMethod;

    /**
     * 用来处理连接断开时的方法
     */
    private Method onCloseMethod;

    /**
     * 握手处理函数
     */
    private Method beforeHandshake;

    private Method onErrorMethod;

    private Method onBinaryMethod;

    private Method onEventMethod;


    private MethodParameter[] beforeHandshakeParameters;
    private MethodParameter[] onOpenParameters;
    private MethodParameter[] onCloseParameters;
    private MethodParameter[] onErrorParameters;
    private MethodParameter[] onMessageParameters;
    private MethodParameter[] onBinaryParameters;
    private MethodParameter[] onEventParameters;
    private MethodArgumentResolver[] beforeHandshakeArgResolvers;
    private MethodArgumentResolver[] onOpenArgResolvers;
    private MethodArgumentResolver[] onCloseArgResolvers;
    private MethodArgumentResolver[] onErrorArgResolvers;
    private MethodArgumentResolver[] onMessageArgResolvers;
    private MethodArgumentResolver[] onBinaryArgResolvers;
    private MethodArgumentResolver[] onEventArgResolvers;
    private ApplicationContext applicationContext;
    private AbstractBeanFactory beanFactory;

    public WebsocketMethodMapping(ApplicationContext context, AbstractBeanFactory beanFactory) throws WebsocketCommonException {
        this.applicationContext = context;
        this.beanFactory = beanFactory;
    }

    public void buildParameters() throws WebsocketCommonException {
        beforeHandshakeParameters = getParameters(beforeHandshake);
        onOpenParameters = getParameters(onOpenMethod);
        onCloseParameters = getParameters(onCloseMethod);
        onMessageParameters = getParameters(onMessageMethod);
        onErrorParameters = getParameters(onErrorMethod);
        onBinaryParameters = getParameters(onBinaryMethod);
        onEventParameters = getParameters(onEventMethod);
        beforeHandshakeArgResolvers = getResolvers(beforeHandshakeParameters);
        onOpenArgResolvers = getResolvers(onOpenParameters);
        onCloseArgResolvers = getResolvers(onCloseParameters);
        onMessageArgResolvers = getResolvers(onMessageParameters);
        onErrorArgResolvers = getResolvers(onErrorParameters);
        onBinaryArgResolvers = getResolvers(onBinaryParameters);
        onEventArgResolvers = getResolvers(onEventParameters);
    }

    private Object[] getMethodArgumentValues(Channel channel, Object object, MethodParameter[] parameters, MethodArgumentResolver[] resolvers) throws Exception {
        Object[] objects = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            MethodArgumentResolver resolver = resolvers[i];
            if(resolver == null) {
                continue;
            }
            Object arg = resolver.resolveArgument(parameter, channel, object);
            objects[i] = arg;
        }
        return objects;
    }

    public Object[] getBeforeHandshakeArgs(Channel channel, FullHttpRequest req) throws Exception {
        return getMethodArgumentValues(channel, req, beforeHandshakeParameters, beforeHandshakeArgResolvers);
    }

    public Object[] getOnOpenArgs(Channel channel, FullHttpRequest req) throws Exception {
        return getMethodArgumentValues(channel, req, onOpenParameters, onOpenArgResolvers);
    }

    public Object[] getOnCloseArgs(Channel channel) throws Exception {
        return getMethodArgumentValues(channel, null, onCloseParameters, onCloseArgResolvers);
    }

    public Object[] getOnErrorArgs(Channel channel, Throwable throwable) throws Exception {
        return getMethodArgumentValues(channel, throwable, onErrorParameters, onErrorArgResolvers);
    }

    public Object[] getOnMessageArgs(Channel channel, TextWebSocketFrame textWebSocketFrame) throws Exception {
        return getMethodArgumentValues(channel, textWebSocketFrame, onMessageParameters, onMessageArgResolvers);
    }

    public Object[] getOnBinaryArgs(Channel channel, BinaryWebSocketFrame binaryWebSocketFrame) throws Exception {
        return getMethodArgumentValues(channel, binaryWebSocketFrame, onBinaryParameters, onBinaryArgResolvers);
    }


    public Object[] getOnEventArgs(Channel channel, Object evt) throws Exception {
        return getMethodArgumentValues(channel, evt, onEventParameters, onEventArgResolvers);
    }

    private MethodArgumentResolver[] getResolvers(MethodParameter[] parameters) throws WebsocketCommonException {
        MethodArgumentResolver[] methodArgumentResolvers = new MethodArgumentResolver[parameters.length];
        List<MethodArgumentResolver> resolvers = getDefaultResolvers();
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            for (MethodArgumentResolver resolver : resolvers) {
                if (resolver.supportsParameter(parameter)) {
                    methodArgumentResolvers[i] = resolver;
                    break;
                }
            }
        }
        return methodArgumentResolvers;
    }

    private List<MethodArgumentResolver> getDefaultResolvers() {
        List<MethodArgumentResolver> resolvers = new ArrayList<>();
        resolvers.add(new SessionMethodArgumentResolver());
        resolvers.add(new HttpHeadersMethodArgumentResolver());
        resolvers.add(new TextMethodArgumentResolver());
        resolvers.add(new ThrowableMethodArgumentResolver());
        resolvers.add(new ByteMethodArgumentResolver());
        resolvers.add(new RequestParamMapMethodArgumentResolver());
        resolvers.add(new RequestParamMethodArgumentResolver(beanFactory));
        resolvers.add(new EventMethodArgumentResolver(beanFactory));
        return resolvers;
    }

    private static MethodParameter[] getParameters(Method m) {
        if (m == null) {
            return new MethodParameter[0];
        }
        int count = m.getParameterCount();
        MethodParameter[] result = new MethodParameter[count];
        for (int i = 0; i < count; i++) {
            MethodParameter methodParameter = new MethodParameter(m, i);
            methodParameter.initParameterNameDiscovery(parameterNameDiscoverer);
            result[i] = methodParameter;
        }
        return result;
    }
}
