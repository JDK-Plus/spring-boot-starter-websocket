package plus.jdk.websocket.annotations;

import org.springframework.context.annotation.Import;
import plus.jdk.websocket.global.WebsocketSelector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(WebsocketSelector.class)
public @interface EnableWebSocket {

}
