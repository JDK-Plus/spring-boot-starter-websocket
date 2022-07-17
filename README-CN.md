
<h3 align="center">这是一款使用netty编写的springboot websocket组件。</h3>
<p align="center">
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/blob/master/LICENSE"><img src="https://img.shields.io/github/license/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/releases"><img src="https://img.shields.io/github/release/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/stargazers"><img src="https://img.shields.io/github/stars/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/network/members"><img src="https://img.shields.io/github/forks/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
</p>


- [English](README-CN.md)


## 如何引入

```xml
<dependency>
    <groupId>plus.jdk</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
    <version>1.0.1</version>
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
```


## 使用示例


### 根据自己的业务实现session认证相关逻辑

#### 定义业务关于session的实现

业务上session的定义必须实现`IWsSession` 接口。

该接口封装了一个`userId`,该参数用户可以自定义其类型。还有一个channel，后续用户与该对象的交互都依赖于这个对象

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
package plus.jdk.broadcast.test.session;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.Getter;
import org.springframework.stereotype.Component;
import plus.jdk.websocket.common.HttpWsRequest;
import plus.jdk.websocket.global.IWSSessionAuthenticator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WSSessionAuthenticator implements IWSSessionAuthenticator<MyWsSession> {

    @Getter
    private final Map<String, MyWsSession> ourWsSessionMap = new ConcurrentHashMap<>();

    @Override
    public MyWsSession authenticate(Channel channel, FullHttpRequest req, String path) throws Exception{
        HttpWsRequest httpWsRequest = new HttpWsRequest(req);
        
        // 此处的uid可根据业务实际情况写入，用于后续的业务逻辑
        String uid = httpWsRequest.getQueryValue("uid");
        if(uid == null) {
            throw new Exception("invalid connect"); // 验证失败，抛出异常
        }
        return new MyWsSession(uid, channel);
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

