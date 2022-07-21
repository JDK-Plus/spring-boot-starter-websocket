package plus.jdk.websocket.global;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;
import plus.jdk.broadcast.broadcaster.UdpBroadcastMessageMonitor;
import plus.jdk.broadcast.broadcaster.UdpMessageBroadcaster;
import plus.jdk.broadcast.broadcaster.model.BroadcastMessage;
import plus.jdk.broadcast.model.Monitor;
import plus.jdk.broadcast.properties.BroadCastProperties;
import plus.jdk.websocket.model.IWsSession;
import plus.jdk.websocket.model.UserChannelData;
import plus.jdk.websocket.properties.WebsocketProperties;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedDeque;


@Slf4j
public class UserChannelConnectSynchronizer implements SchedulingConfigurer, ApplicationRunner {

    private final BeanFactory beanFactory;

    private final WebsocketProperties properties;

    private final UdpBroadcastMessageMonitor udpBroadcastMessageMonitor;

    private final UdpMessageBroadcaster udpMessageBroadcaster;

    private final IWSSessionAuthenticatorManager<?, ? extends IWsSession<?>> clusterChannelManager;

    private final Gson gson = new Gson();

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
    protected void sendBroadcast(Object userId, String path, byte[] data, Monitor[] monitors) {
        UserChannelData channelData = new UserChannelData(userId, path, data);
        byte[] byteData = gson.toJson(channelData).getBytes(StandardCharsets.UTF_8);
        udpMessageBroadcaster.publish(new BroadcastMessage(byteData, Arrays.asList(monitors)));
    }

    /**
     * 同步给集群内其他机器当前机器和哪些用户建立了连接
     */
    public void broadcastAllConnects() {

    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        long fixRate = 30 * 1000L;
        Trigger eventTrigger = new PeriodicTrigger(fixRate);
        scheduledTaskRegistrar.addTriggerTask(this::broadcastAllConnects, eventTrigger);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Thread thread = new Thread(() -> udpBroadcastMessageMonitor.subscribe((ctx, msg) -> {
            String message = new String(msg.getContent());
            log.info("receive {}", new String(msg.getContent()));
            UserChannelData channelData = gson.fromJson(message, UserChannelData.class);
            SessionGroupManager sessionGroupManager = beanFactory.getBean(SessionGroupManager.class);
            ConcurrentLinkedDeque<IWsSession<?>> sessions = sessionGroupManager.getSession(channelData.getUserId(), channelData.getPath());
            for (IWsSession<?> session : sessions) {
                session.sendText(new String(channelData.getData()));
            }
            return true;
        }));
        thread.start();
    }
}
