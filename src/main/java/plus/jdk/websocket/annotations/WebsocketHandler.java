package plus.jdk.websocket.annotations;

import java.lang.annotation.*;

@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface WebsocketHandler {

    /**
     * websocket 请求路径
     * @return 可指定多个路径
     */
    String[] values();

}
