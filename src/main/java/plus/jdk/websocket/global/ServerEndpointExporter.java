package plus.jdk.websocket.global;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import plus.jdk.websocket.WebsocketDispatcher;
import plus.jdk.websocket.annotations.*;
import plus.jdk.websocket.common.ReflectUtils;
import plus.jdk.websocket.model.WebsocketMethodMapping;

import javax.annotation.Resource;
import java.lang.reflect.Modifier;

@Slf4j
public class ServerEndpointExporter extends ApplicationObjectSupport implements SmartInitializingSingleton, BeanFactoryAware, ResourceLoaderAware {

    private final WebsocketDispatcher websocketDispatcher;

    private AbstractBeanFactory beanFactory;

    private ResourceLoader resourceLoader;

    public ServerEndpointExporter(WebsocketDispatcher websocketDispatcher) {
        this.websocketDispatcher = websocketDispatcher;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (!(beanFactory instanceof AbstractBeanFactory)) {
            throw new IllegalArgumentException(
                    "AutowiredAnnotationBeanPostProcessor requires a AbstractBeanFactory: " + beanFactory);
        }
        this.beanFactory = (AbstractBeanFactory) beanFactory;
    }

    //    private Method onEventMethod;
    @SneakyThrows
    @Override
    public void afterSingletonsInstantiated() {
        ApplicationContext context = getApplicationContext();
        assert context != null;
        String[] endpointBeanNames = context.getBeanNamesForAnnotation(WebsocketHandler.class);
        for (String beanName : endpointBeanNames) {
            Object beanObject = context.getBean(beanName);
            WebsocketHandler websocketHandler = beanObject.getClass().getAnnotation(WebsocketHandler.class);
            for (String path : websocketHandler.values()) {
                WebsocketMethodMapping websocketMethodMapping = new WebsocketMethodMapping(context, beanFactory);
                websocketMethodMapping.setPath(path);
                websocketMethodMapping.setBeanObject(beanObject);
                websocketMethodMapping.setOnOpenMethod(ReflectUtils.getFirstAnnotatedMethod(beanObject, OnWsOpen.class, method -> Modifier.isPublic(method.getModifiers())));
                websocketMethodMapping.setOnMessageMethod(ReflectUtils.getFirstAnnotatedMethod(beanObject, OnWsMessage.class, method -> Modifier.isPublic(method.getModifiers())));
                websocketMethodMapping.setOnCloseMethod(ReflectUtils.getFirstAnnotatedMethod(beanObject, OnWsClose.class, method -> Modifier.isPublic(method.getModifiers())));
                websocketMethodMapping.setBeforeHandshake(ReflectUtils.getFirstAnnotatedMethod(beanObject, OnWsHandshake.class, method -> Modifier.isPublic(method.getModifiers())));
                websocketMethodMapping.setOnErrorMethod(ReflectUtils.getFirstAnnotatedMethod(beanObject, OnWsError.class, method -> Modifier.isPublic(method.getModifiers())));
                websocketMethodMapping.setOnBinaryMethod(ReflectUtils.getFirstAnnotatedMethod(beanObject, OnWsBinary.class, method -> Modifier.isPublic(method.getModifiers())));
                websocketMethodMapping.setOnEventMethod(ReflectUtils.getFirstAnnotatedMethod(beanObject, OnWsEvent.class, method -> Modifier.isPublic(method.getModifiers())));
                websocketMethodMapping.buildParameters();
                websocketDispatcher.registerEndpoint(path, websocketMethodMapping);
            }
        }
        websocketDispatcher.startSocketServer();
        log.info("source websocket handler success");
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
