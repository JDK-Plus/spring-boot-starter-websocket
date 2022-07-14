package plus.jdk.websocket.global;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import plus.jdk.websocket.WebsocketDispatcher;
import plus.jdk.websocket.properties.WebsocketProperties;

@Configuration
@ConditionalOnMissingBean(ServerEndpointExporter.class)
public class WebsocketSelector {


    @Bean
    public ServerEndpointExporter serverEndpointExporter(WebsocketProperties properties) {
        WebsocketDispatcher websocketDispatcher = new WebsocketDispatcher(properties);
        return new ServerEndpointExporter(websocketDispatcher);
    }
}
