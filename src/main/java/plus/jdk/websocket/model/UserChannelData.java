package plus.jdk.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserChannelData {

    private Object userId;

    private String path;

    private byte[] data;
}
