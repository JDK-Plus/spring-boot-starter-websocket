package plus.jdk.websocket.global;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import plus.jdk.websocket.properties.WebsocketProperties;

import javax.annotation.Resource;

@Configuration
@ConditionalOnMissingBean(ServerEndpointExporter.class)
public class WebsocketSelector {

}
