package plus.jdk.websocket.global;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import plus.jdk.websocket.properties.WebsocketProperties;

import javax.annotation.Resource;

@Configuration
@ConditionalOnMissingBean(ServerEndpointExporter.class)
@ConditionalOnProperty(prefix = "plus.jdk.websocket", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(WebsocketProperties.class)
public class WebsocketSelector extends WebApplicationObjectSupport implements BeanFactoryAware, WebMvcConfigurer {

    private BeanFactory beanFactory;

    private WebsocketProperties properties;

    public WebsocketSelector(WebsocketProperties properties) {
        this.properties = properties;
    }

    @Bean
    public SessionGroupManager SessionGroupManager() {
        return new SessionGroupManager(beanFactory, properties);
    }

    @Bean
    public WebsocketDispatcher WebsocketDispatcher(){
        return new WebsocketDispatcher(properties, beanFactory);
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter(WebsocketDispatcher websocketDispatcher) {
        return new ServerEndpointExporter(websocketDispatcher);
    }

    @Bean
    public UserChannelConnectSynchronizer getHeartbeatSynchronizer() {
        return new UserChannelConnectSynchronizer(beanFactory, properties);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}