package plus.jdk.websocket.global;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import plus.jdk.broadcast.model.Monitor;
import plus.jdk.websocket.model.IWsSession;
import plus.jdk.websocket.properties.WebsocketProperties;

public interface IWSSessionAuthenticatorManager<U, T extends IWsSession<U>> {

    /**
     * 当建立连接时鉴权. 若用户无权限，抛出异常即可，否则返回合法的session对象
     */
    T authenticate(Channel channel, FullHttpRequest req, String path, WebsocketProperties properties) throws Exception;

    /**
     * 当连接断开时,可以通过该回调释放存储的连接数据
     * @param session
     */
    default void onSessionDestroy(IWsSession<?> session, String path, WebsocketProperties properties) {

    }

    /**
     * 返回用户和哪些机器建立了连接
     */
    default Monitor[] getUserConnectedMachine(U userId, String path, WebsocketProperties properties) {
        return new Monitor[]{new Monitor("127.0.0.1", properties.getBroadcastMonitorPort())};
    }

    /**
     * 返回当前集群有哪些机器
     */
    default Monitor[] getAllUdpMonitors(WebsocketProperties properties) {
        return new Monitor[]{};
    }
}
