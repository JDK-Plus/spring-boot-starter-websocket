package plus.jdk.websocket.global;

import com.google.protobuf.ByteString;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import plus.jdk.broadcast.broadcaster.UdpBroadcastMessageMonitor;
import plus.jdk.broadcast.broadcaster.UdpMessageBroadcaster;
import plus.jdk.broadcast.broadcaster.model.BroadcastMessage;
import plus.jdk.broadcast.model.Monitor;
import plus.jdk.broadcast.properties.BroadCastProperties;
import plus.jdk.websocket.model.IWsSession;
import plus.jdk.websocket.properties.WebsocketProperties;
import plus.jdk.websocket.protoc.MessageType;
import plus.jdk.websocket.protoc.WsMessage;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedDeque;


@Slf4j
public class UserChannelConnectSynchronizer implements ApplicationRunner {

    private final BeanFactory beanFactory;

    private final WebsocketProperties properties;

    private final UdpBroadcastMessageMonitor udpBroadcastMessageMonitor;

    private final UdpMessageBroadcaster udpMessageBroadcaster;

    private final IWSSessionAuthenticatorManager<?, ? extends IWsSession<?>> clusterChannelManager;


    /**
     * 集群内有哪些机器,port设置为开启的udp端口即可
     */
    private final Monitor[] clusterMonitors;

    public UserChannelConnectSynchronizer(BeanFactory beanFactory, WebsocketProperties properties) {
        this.beanFactory = beanFactory;
        this.properties = properties;
        BroadCastProperties broadCastProperties = new BroadCastProperties();
        broadCastProperties.setMonitorPort(properties.getBroadcastMonitorPort());
        udpBroadcastMessageMonitor = new UdpBroadcastMessageMonitor(broadCastProperties);
        udpMessageBroadcaster = new UdpMessageBroadcaster(broadCastProperties);
        clusterChannelManager = beanFactory.getBean(properties.getSessionAuthenticator());
        clusterMonitors = clusterChannelManager.getAllUdpMonitors(properties);
    }

    /**
     * 向目标机器发送udp报文
     */
    protected void sendBroadcast(Object userId, String path, byte[] data, Monitor[] monitors, MessageType messageType) {
        WsMessage.Builder builder = WsMessage.newBuilder();
        builder.setData(ByteString.copyFrom(data));
        builder.setUid(userId == null ? "" : userId.toString());
        builder.setPath(path);
        builder.setType(messageType);
        WsMessage wsMessage = builder.build();
        udpMessageBroadcaster.publish(new BroadcastMessage(wsMessage.toByteArray(), Arrays.asList(monitors)));
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (properties.getBroadcastMonitorPort() <= 0) {
            return;
        }
        Thread thread = new Thread(() -> udpBroadcastMessageMonitor.subscribe((ctx, msg) -> {
            WsMessage wsMessage = WsMessage.parseFrom(msg.getContent());
            SessionGroupManager sessionGroupManager = beanFactory.getBean(SessionGroupManager.class);
            ConcurrentLinkedDeque<IWsSession<?>> sessions = sessionGroupManager.getSession(wsMessage.getUid(), wsMessage.getPath());
            if (properties.getPrintBroadcastMessage()) {
                log.info("receive broadcast message: {}", wsMessage);
            }
            for (IWsSession<?> session : sessions) {
                ChannelFuture future = null;
                if (MessageType.MESSAGE_TYPE_TEXT.equals(wsMessage.getType())) {
                    future = session.sendText(new String(wsMessage.getData().toByteArray()));
                }
                if (MessageType.MESSAGE_TYPE_BINARY.equals(wsMessage.getType())) {
                    future = session.sendBinary(wsMessage.getData().toByteArray());
                }
                if (future == null) {
                    continue;
                }
                future.addListener((ChannelFutureListener) channelFuture -> {
                    if(properties.getMessagePushPromise() == null) {
                        return;
                    }
                    try {
                        IBroadMessagePromise promise = beanFactory.getBean(properties.getMessagePushPromise());
                        promise.onCompletion(channelFuture.isSuccess(), wsMessage,  session);
                    }catch(Exception e) {
                        log.error(e.getMessage());
                    }
                });
            }
            return true;
        }));
        Runtime.getRuntime().addShutdownHook(thread);
    }
}
