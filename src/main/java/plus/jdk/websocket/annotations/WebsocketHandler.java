package plus.jdk.websocket.annotations;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

@Bean
@Service
@Inherited
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WebsocketHandler {

    /**
     * websocket 请求路径
     * @return 可指定多个路径
     */
    String[] values();

}
