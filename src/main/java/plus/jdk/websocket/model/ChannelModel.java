package plus.jdk.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChannelModel {

    /**
     * 当前的uid
     */
    private Object userId;

    /**
     * websocket路径
     */
    private String path;
}
