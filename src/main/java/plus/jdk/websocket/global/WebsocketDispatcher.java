package plus.jdk.websocket.global;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanFactory;
import plus.jdk.websocket.model.IWsSession;
import plus.jdk.websocket.properties.WebsocketProperties;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class WebsocketDispatcher {

    private final WebsocketProperties properties;

    private static final AttributeKey<Object> POJO_KEY = AttributeKey.valueOf("WEBSOCKET_IMPLEMENT");

    public static final AttributeKey<IWsSession<?>> SESSION_KEY = AttributeKey.valueOf("WEBSOCKET_SESSION");

    private static final AttributeKey<String> PATH_KEY = AttributeKey.valueOf("WEBSOCKET_PATH");

    public static final AttributeKey<Map<String, String>> URI_TEMPLATE = AttributeKey.valueOf("WEBSOCKET_URI_TEMPLATE");

    public static final AttributeKey<Map<String, List<String>>> REQUEST_PARAM = AttributeKey.valueOf("WEBSOCKET_REQUEST_PARAM");

    private final BeanFactory beanFactory;

    @Getter
    private final ConcurrentHashMap<String, WebsocketMethodMapping> websocketMethodMap = new ConcurrentHashMap<>();

    public WebsocketDispatcher(WebsocketProperties properties, BeanFactory beanFactory) {
        this.properties = properties;
        this.beanFactory = beanFactory;
    }

    public void registerEndpoint(String path, WebsocketMethodMapping desc) {
        websocketMethodMap.put(path, desc);
    }

    public void startSocketServer() {
        String[] corsOrigins = properties.getCorsOrigins();
        Boolean corsAllowCredentials = properties.getCorsAllowCredentials();
        CorsConfig corsConfig = createCorsConfig(corsOrigins, corsAllowCredentials);
        NioEventLoopGroup master = new NioEventLoopGroup(properties.getBossLoopGroupThreads());
        NioEventLoopGroup worker = new NioEventLoopGroup(properties.getWorkerLoopGroupThreads());
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(master, worker);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMillis());
        bootstrap.option(ChannelOption.SO_BACKLOG, properties.getSO_BACKLOG());
        bootstrap.option(ChannelOption.WRITE_SPIN_COUNT, properties.getWriteSpinCount());
        bootstrap.handler(new LoggingHandler(properties.getLogLevel()));
        WebsocketDispatcher websocketDispatcher = this;
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("logging", new LoggingHandler("DEBUG"));//设置log监听器，并且日志级别为debug，方便观察运行流程
                ch.pipeline().addLast("http-codec", new HttpServerCodec());//设置解码器
                ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));//聚合器，使用websocket会用到
                ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());//用于大数据的分区传输
                if (corsConfig != null) {
                    ch.pipeline().addLast(new CorsHandler(corsConfig));
                }
                ch.pipeline().addLast("handler", new HttpServerHandler(properties, websocketDispatcher,  worker, beanFactory));//自定义的业务handler
            }
        });

        if (properties.getChildOptionSoRcvBuf() != -1) {
            bootstrap.childOption(ChannelOption.SO_RCVBUF, properties.getChildOptionSoRcvBuf() );
        }

        if (properties.getChildOptionSoSndBuf() != -1) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, properties.getChildOptionSoSndBuf());
        }

        ChannelFuture channelFuture;
        channelFuture = bootstrap.bind(properties.getPort());
        log.info("start websocket server {}", properties.getPort());
        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            master.shutdownGracefully().syncUninterruptibly();
            worker.shutdownGracefully().syncUninterruptibly();
        }));
    }

    private CorsConfig createCorsConfig(String[] corsOrigins, Boolean corsAllowCredentials) {
        if (corsOrigins == null || corsOrigins.length == 0) {
            return null;
        }
        CorsConfigBuilder corsConfigBuilder = null;
        for (String corsOrigin : corsOrigins) {
            if ("*".equals(corsOrigin)) {
                corsConfigBuilder = CorsConfigBuilder.forAnyOrigin();
                break;
            }
        }
        if (corsConfigBuilder == null) {
            corsConfigBuilder = CorsConfigBuilder.forOrigins(corsOrigins);
        }
        if (corsAllowCredentials != null && corsAllowCredentials) {
            corsConfigBuilder.allowCredentials();
        }
        corsConfigBuilder.allowNullOrigin();
        return corsConfigBuilder.build();
    }

    /**
     * 判定是否实现握手相关函数
     */
    public boolean hasBeforeHandshake(Channel channel, String path) {
        WebsocketMethodMapping websocketMethodMapping = this.getWebsocketMethodMap().get(path);
        if(websocketMethodMapping == null) {
            return false;
        }
        return websocketMethodMapping.getBeforeHandshake()!=null;
    }

    /**
     * 执行握手
     */
    public void doBeforeHandshake(Channel channel, FullHttpRequest req, String path) throws Exception {
        WebsocketMethodMapping methodMapping = websocketMethodMap.get(path);
        if(methodMapping == null) {
            return;
        }
        Object implement = methodMapping.getBeanObject();
        channel.attr(POJO_KEY).set(implement);
        setSession(channel, req, path);
        channel.attr(PATH_KEY).set(path);
        Method beforeHandshake = methodMapping.getBeforeHandshake();
        if (beforeHandshake != null) {
            try {
                beforeHandshake.invoke(implement, methodMapping.getBeforeHandshakeArgs(channel, req));
            } catch (TypeMismatchException e) {
                throw e;
            } catch (Throwable t) {
                log.error("{}", t.getMessage());
            }
        }
    }

    public void doOnOpen(Channel channel, FullHttpRequest req, String path) throws Exception {
        WebsocketMethodMapping methodMapping = websocketMethodMap.get(path);
        if(methodMapping == null) {
            return;
        }
        Object implement = channel.attr(POJO_KEY).get();
        if (implement==null){
            implement = methodMapping.getBeanObject();
            setSession(channel, req, path);
        }

        Method onOpenMethod = methodMapping.getOnOpenMethod();
        if (onOpenMethod != null) {
            try {
                onOpenMethod.invoke(implement, methodMapping.getOnOpenArgs(channel, req));
            } catch (TypeMismatchException e) {
                throw e;
            } catch (Throwable t) {
                log.error("{}", t.getMessage());
            }
        }
    }

    private void setSession(Channel channel, FullHttpRequest req, String path) throws Exception {
        IWSSessionAuthenticatorManager<?, ?> authenticator = beanFactory.getBean(properties.getSessionAuthenticator());
        SessionGroupManager sessionGroupManager = beanFactory.getBean(SessionGroupManager.class);
        IWsSession<?> wsSession = authenticator.authenticate(channel, req, path, properties);
        channel.attr(SESSION_KEY).set(wsSession);
        sessionGroupManager.addSession(path, wsSession);
    }

    protected void releaseSession(ChannelHandlerContext ctx) {
        try{
            SessionGroupManager sessionGroupManager = beanFactory.getBean(SessionGroupManager.class);
            sessionGroupManager.releaseChannel(ctx);
        }catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public void doOnClose(Channel channel) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        String path = attrPath.get();
        WebsocketMethodMapping methodMapping = websocketMethodMap.get(path);
        if(methodMapping == null) {
            return;
        }
        if (methodMapping.getOnCloseMethod() != null) {
            if (!channel.hasAttr(SESSION_KEY)) {
                return;
            }
            Object implement = channel.attr(POJO_KEY).get();
            try {
                methodMapping.getOnCloseMethod().invoke(implement,
                        methodMapping.getOnCloseArgs(channel));
            } catch (Throwable t) {
                log.error("{}", t.getMessage());
            }
        }
    }


    public void doOnError(Channel channel, Throwable throwable) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        String path = attrPath.get();
        WebsocketMethodMapping methodMapping = websocketMethodMap.get(path);
        if(methodMapping == null) {
            return;
        }
        if (methodMapping.getOnErrorMethod() != null) {
            if (!channel.hasAttr(SESSION_KEY)) {
                return;
            }
            Object implement = channel.attr(POJO_KEY).get();
            try {
                Method method = methodMapping.getOnErrorMethod();
                Object[] args = methodMapping.getOnErrorArgs(channel, throwable);
                method.invoke(implement, args);
            } catch (Throwable t) {
                log.error("{}", t.getMessage());
            }
        }
    }

    public void doOnMessage(Channel channel, WebSocketFrame frame) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        String path = attrPath.get();
        WebsocketMethodMapping methodMapping = websocketMethodMap.get(path);
        if(methodMapping == null) {
            return;
        }
        if (methodMapping.getOnMessageMethod() != null) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            Object implement = channel.attr(POJO_KEY).get();
            try {
                methodMapping.getOnMessageMethod().invoke(implement, methodMapping.getOnMessageArgs(channel, textFrame));
            } catch (Throwable t) {
                log.error("{}", t.getMessage());
            }
        }
    }

    public void doOnBinary(Channel channel, WebSocketFrame frame) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        String path = attrPath.get();
        WebsocketMethodMapping methodMapping = websocketMethodMap.get(path);
        if(methodMapping == null) {
            return;
        }
        if (methodMapping.getOnBinaryMethod() != null) {
            BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) frame;
            Object implement = channel.attr(POJO_KEY).get();
            try {
                methodMapping.getOnBinaryMethod().invoke(implement, methodMapping.getOnBinaryArgs(channel, binaryWebSocketFrame));
            } catch (Throwable t) {
                log.error("{}", t.getMessage());
            }
        }
    }

    public void doOnEvent(Channel channel, Object evt) {
        Attribute<String> attrPath = channel.attr(PATH_KEY);
        String path = attrPath.get();
        WebsocketMethodMapping methodMapping = websocketMethodMap.get(path);
        if(methodMapping == null) {
            return;
        }
        if (methodMapping.getOnEventMethod() != null) {
            if (!channel.hasAttr(SESSION_KEY)) {
                return;
            }
            Object implement = channel.attr(POJO_KEY).get();
            try {
                methodMapping.getOnEventMethod().invoke(implement, methodMapping.getOnEventArgs(channel, evt));
            } catch (Throwable t) {
                log.error("{}", t.getMessage());
            }
        }
    }

}
