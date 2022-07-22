package plus.jdk.websocket.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import plus.jdk.websocket.global.UserChannelConnectSynchronizer;
import plus.jdk.websocket.global.ServerEndpointExporter;
import plus.jdk.websocket.global.SessionGroupManager;
import plus.jdk.websocket.global.WebsocketDispatcher;
import plus.jdk.websocket.annotations.*;
import plus.jdk.websocket.properties.WebsocketProperties;

import javax.annotation.Resource;

@Slf4j
@Configuration
@EnableWebSocket
@ConditionalOnProperty(prefix = "plus.jdk.websocket", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(WebsocketProperties.class)
public class WebsocketAutoConfiguration extends WebApplicationObjectSupport implements InitializingBean, BeanFactoryAware, DisposableBean, WebMvcConfigurer {

    @Resource
    private WebsocketDispatcher websocketDispatcher;

    private BeanFactory beanFactory;

    public WebsocketAutoConfiguration(WebsocketProperties properties) {
    }

    @Bean
    public SessionGroupManager SessionGroupManager(WebsocketProperties properties) {
        return new SessionGroupManager(beanFactory, properties);
    }

    @Bean
    public WebsocketDispatcher WebsocketDispatcher(WebsocketProperties properties){
        return new WebsocketDispatcher(properties, beanFactory);
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter(WebsocketProperties properties) {
        return new ServerEndpointExporter(websocketDispatcher);
    }

    @Bean
    public UserChannelConnectSynchronizer getHeartbeatSynchronizer(WebsocketProperties properties) {
        return new UserChannelConnectSynchronizer(beanFactory, properties);
    }

    @Override
    public void destroy() {

    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
