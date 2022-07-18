package plus.jdk.websocket.global;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import plus.jdk.websocket.model.ChannelModel;
import plus.jdk.websocket.model.IWsSession;
import plus.jdk.websocket.properties.WebsocketProperties;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
public class SessionGroupManager {

    private final ConcurrentHashMap<Channel, ChannelModel> channelModelMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Object, ConcurrentHashMap<String, ConcurrentLinkedDeque<IWsSession<?>>>> sessionMap = new ConcurrentHashMap<>();

    private final BeanFactory beanFactory;

    private final WebsocketProperties properties;

    public SessionGroupManager(BeanFactory beanFactory, WebsocketProperties properties) {
        this.beanFactory = beanFactory;
        this.properties = properties;
    }

    protected void addSession(String path, IWsSession<?> iWsSession) {
        Object uid = iWsSession.getUserId();
        sessionMap.putIfAbsent(uid, new ConcurrentHashMap<>());
        sessionMap.get(uid).putIfAbsent(path, new ConcurrentLinkedDeque<>());
        sessionMap.get(uid).get(path).add(iWsSession);
        channelModelMap.put(iWsSession.getChannel(), new ChannelModel(uid, path));
    }

    public <T> ConcurrentLinkedDeque<IWsSession<?>> getSession(T userId, String path) {
        ConcurrentHashMap<String, ConcurrentLinkedDeque<IWsSession<?>>> userSessionMap = sessionMap.get(userId);
        if (userSessionMap == null || userSessionMap.get(path) == null) {
            return new ConcurrentLinkedDeque<>();
        }
        return sessionMap.get(userId).get(path);
    }

    protected void releaseChannel(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        ChannelModel channelModel = channelModelMap.get(channel);
        Object uid = channelModel.getUserId();
        String path = channelModel.getPath();
        sessionMap.putIfAbsent(uid, new ConcurrentHashMap<>());
        sessionMap.get(uid).putIfAbsent(path, new ConcurrentLinkedDeque<>());
        ConcurrentLinkedDeque<IWsSession<?>> wsSessionsDeque = sessionMap.get(uid).get(path);
        if (wsSessionsDeque == null) {
            return;
        }
        wsSessionsDeque.removeIf(iWsSession -> {
            if(iWsSession.getChannel() == channel) {
                try {
                    IWSSessionAuthenticator<?> iwsSessionAuthenticator = beanFactory.getBean(properties.getSessionAuthenticator());
                    iwsSessionAuthenticator.onSessionDestroy((IWsSession) iWsSession);
                }catch(Exception e) {
                    log.error(e.getMessage());
                }
                return true;
            }
            return false;
        });
    }

}
