package plus.jdk.websocket.global;

import io.netty.channel.Channel;
import plus.jdk.websocket.model.IWsSession;
import plus.jdk.websocket.protoc.WsMessage;


public interface IBroadMessagePromise {

    void onCompletion(boolean success, WsMessage wsMessage, IWsSession<?> session);
}
