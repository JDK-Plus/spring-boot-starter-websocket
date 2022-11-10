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
public class WebsocketAutoConfiguration {

}
