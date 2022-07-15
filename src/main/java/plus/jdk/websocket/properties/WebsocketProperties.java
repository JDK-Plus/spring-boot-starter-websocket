package plus.jdk.websocket.properties;

import io.netty.handler.logging.LogLevel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import plus.jdk.websocket.global.DefaultSessionAuthenticator;
import plus.jdk.websocket.global.IWSSessionAuthenticator;

@Data
@ConfigurationProperties(prefix = "plus.jdk.websocket")
public class WebsocketProperties {

    /**
     * 是否开启功能
     */
    private Boolean enabled = false;

    /**
     * boss线程池线程数，默认为1
     */
    private Integer bossLoopGroupThreads = 0;

    /**
     * worker线程池线程数,若不指定则默认为CPU核心数 * 2
     */
    private Integer workerLoopGroupThreads = 0;

    /**
     * 跨域的header头
     */
    private String[] corsOrigins;

    /**
     * 是否允许跨域
     */
    private Boolean corsAllowCredentials = false;

    /**
     * 监听哪个端口
     */
    private Integer port = 10300;

    /**
     * 监听的host
     */
    private String host = "";

    private Boolean useEventExecutorGroup = true;

    private Integer eventExecutorGroupThreads = 0;

    /**
     * 连接超时时间
     */
    private Integer connectTimeoutMillis;

    /**
     * 指定了内核为此套接口排队的最大连接个数；
     */
    private Integer SO_BACKLOG;

    /**
     * 旋转计数用于控制每次Netty写入操作调用基础socket.write(...)的次数
     */
    private Integer writeSpinCount;

    /**
     * 日志等级
     */
    private LogLevel logLevel = LogLevel.DEBUG;

    private Integer readerIdleTimeSeconds = 0;

    private Integer writerIdleTimeSeconds = 0;

    private Integer allIdleTimeSeconds = 0;

    private Integer maxFramePayloadLength = 65536;

    private Boolean useCompressionHandler = false;

    private Integer childOptionSoRcvBuf = -1;

    private Integer childOptionSoSndBuf = -1;

    /**
     * 认证器
     */
    private Class<? extends IWSSessionAuthenticator> sessionAuthenticator = DefaultSessionAuthenticator.class;
}
