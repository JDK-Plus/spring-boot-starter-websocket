<img align="center" src="https://jdk.plus/img/jdk-plus.png" alt="drawing" style="width:100%;"/>
<h3 align="center">这是一款使用netty编写的springboot websocket 集群化消息推送的组件。</h3>
<p align="center">
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/blob/master/LICENSE"><img src="https://img.shields.io/github/license/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/releases"><img src="https://img.shields.io/github/release/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/stargazers"><img src="https://img.shields.io/github/stars/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/network/members"><img src="https://img.shields.io/github/forks/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
</p>
<p align="center">这是一款支持集群广播的使用netty编写的websocket组件,完美解决websocket和用户单机建立连接无法和整个业务集群通信的问题</p>


- [English](README-CN.md)


## 如何引入

```xml
<dependency>
    <groupId>plus.jdk</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
    <version>1.0.7</version>
</dependency>
```
## 配置

```
plus.jdk.websocket.enabled=true

# 指定host
plus.jdk.websocket.host=0.0.0.0

# 指定websocket端口
plus.jdk.websocket.port=10001


# 指定自定义实现的验证器
plus.jdk.websocket.session-authenticator=plus.jdk.broadcast.test.session.WSSessionAuthenticator

# boss线程池线程数，默认为1
plus.jdk.websocket.boss-loop-group-threads=1

# worker线程池线程数,若不指定则默认为CPU核心数 * 2
plus.jdk.websocket.worker-loop-group-threads=5

# 是否需允许跨域
plus.jdk.websocket.cors-allow-credentials=true

# 跨域的header头
plus.jdk.websocket.cors-origins[0]=""

# 是否使用 NioEventLoopGroup 来处理请求
plus.jdk.websocket.use-event-executor-group=true

# 指定 NioEventLoopGroup 线程池数量
plus.jdk.websocket.event-executor-group-threads=0

# 连接超时时间
#plus.jdk.websocket.connect-timeout-millis=

# 指定了内核为此套接口排队的最大连接个数
#plus.jdk.websocket.SO_BACKLOG=

# 旋转计数用于控制每次Netty写入操作调用基础socket.write(...)的次数
#plus.jdk.websocket.write-spin-count=

# 日志等级
plus.jdk.websocket.log-level=debug

# udp广播监听端口
plus.jdk.websocket.broadcast-monitor-port=10300

# udp广播监听端口，若小于等于0，则不监听消息
plus.jdk.websocket.broadcast-monitor-port=10300

# 是否将接收到的UDP广播信息打印到日志中
plus.jdk.websocket.print-broadcast-message=true
```


## 使用示例


### 根据自己的业务实现session认证相关逻辑

#### 定义业务关于session的实现

业务上session的定义必须实现`IWsSession` 接口。

该接口封装了一个`userId`,该参数用户可以自定义其类型。还有一个channel，是当前机器与用户建立的连接

```java
package plus.jdk.broadcast.test.session;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;
import plus.jdk.websocket.model.IWsSession;

@Data
@AllArgsConstructor
public class MyWsSession implements IWsSession<String> {

    private String userId;

    private Channel channel;
}
```

#### 自定义`Session`验证器

验证器必须实现`IWSSessionAuthenticator`接口，使用示例如下：

```java
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import org.springframework.stereotype.Component;
import plus.jdk.broadcast.model.Monitor;
import plus.jdk.websocket.common.HttpWsRequest;
import plus.jdk.websocket.global.IWSSessionAuthenticatorManager;
import plus.jdk.websocket.model.IWsSession;
import plus.jdk.websocket.properties.WebsocketProperties;

@Component
public class WSSessionAuthenticator implements IWSSessionAuthenticatorManager<String, MyWsSession> {

    /**
     * 握手阶段验证session信息，若验证不通过，直接抛出异常终止握手流程。
     * 若验证成功，则返回自定义的session，并使用redis之类的公用存储服务记录当前用户和哪台机器建立了连接
     */
    @Override
    public MyWsSession authenticate(Channel channel, FullHttpRequest req, String path, WebsocketProperties properties) {
        HttpWsRequest httpWsRequest = new HttpWsRequest(req);
        String uid = httpWsRequest.getQueryValue("uid");
        return new MyWsSession(uid, channel);
    }

    /**
     * 当连接断开时，销毁session的回调
     */
    @Override
    public void onSessionDestroy(IWsSession<?> session, String path, WebsocketProperties properties) {

    }

    /**
     * 返回当前用户和哪些机器建立了连接，需要向这些机器发送广播推送消息
     */
    @Override
    public Monitor[] getUserConnectedMachine(String userId, String path, WebsocketProperties properties) {
        return new Monitor[]{new Monitor("127.0.0.1", properties.getBroadcastMonitorPort())};
    }
}
```

**通过配置指定验证器组件**

```
plus.jdk.websocket.session-authenticator=plus.jdk.broadcast.test.session.WSSessionAuthenticator
```


### 编写相关业务逻辑

如上文，当实现Session认证后，即可继续编写websocket的业务逻辑了。

```java
package plus.jdk.broadcast.test.websocket;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import plus.jdk.broadcast.test.session.MyWsSession;
import plus.jdk.websocket.annotations.*;

@Component
@WebsocketHandler(values = {"/ws/message"})
public class DemoHandler {

    @OnWsHandshake
    public void doHandshake(FullHttpRequest req, MyWsSession session) {
    }

    @OnWsOpen
    public void onOpen(Channel channel, FullHttpRequest req, MyWsSession session, HttpHeaders headers, @RequestParam String uid) {
        session.sendText("onOpen" + System.currentTimeMillis());
    }

    @OnWsMessage
    public void onWsMessage(MyWsSession session, String data) {
        session.sendText("onWsMessage" + System.currentTimeMillis());
        session.sendText("receive data" + data);
        session.sendText("onWsMessage, id:" + session.getChannel().id().asShortText());
    }

    @OnWsEvent
    public void onWsEvent(Object data, MyWsSession session) {
        session.sendText("onWsEvent" + System.currentTimeMillis());
    }

    @OnWsBinary
    public void OnWsBinary(MyWsSession session, byte[] data) {
        session.sendText("OnWsBinary" + System.currentTimeMillis());
    }

    @OnWsError
    public void onWsError(MyWsSession session, Throwable throwable) {
        session.sendText("onWsError" + throwable.getMessage());
    }

    @OnWsClose
    public void onWsClose(MyWsSession session){
        session.sendText("onWsClose" + System.currentTimeMillis());
    }
}
```

### 使用websocket连接主动向用户客户端推送消息

```java
package plus.jdk.broadcast.test.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import plus.jdk.websocket.global.SessionGroupManager;
import plus.jdk.websocket.model.IWsSession;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentLinkedDeque;


@RestController
public class MessageController {

    /**
     * 该bean实例已经在底层封装好
     */
    @Resource
    private SessionGroupManager sessionGroupManager;

    @RequestMapping(value = "/message/send", method = {RequestMethod.GET})
    public Object sendMessage(@RequestParam String uid, @RequestParam String content){
        // 调用sessionGroupManager.getSession()函数获取当前用户在该实例中的所有连接
        // 你可以在 IWSSessionAuthenticator 的实现中自行实现自己的session定义，将消息分发给不同的设备
        // 或向远端上报当前用户的连接到底在哪些机器上
        ConcurrentLinkedDeque<IWsSession<?>> sessions = sessionGroupManager.getSession(uid, "/ws/message");

        // 发送文本消息
        // 如果你按要求实现了IWSSessionAuthenticatorManager接口中的getUserConnectedMachine方法，那么将会向已经和用户建立连接的机器发送广播，推送消息

        sessionGroupManager.sendText(uid, "/ws/message", content);

        // 发送二进制消息
        sessionGroupManager.sendBinary(uid, "/ws/message", content.getBytes(StandardCharsets.UTF_8));
        return "success";
    }
}
```
