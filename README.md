<img align="center" src="https://jdk.plus/img/jdk-plus.png" alt="drawing" style="width:100%;"/>
<h3 align="center">A springboot websocket component written using netty。</h3>
<p align="center">
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/blob/master/LICENSE"><img src="https://img.shields.io/github/license/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/releases"><img src="https://img.shields.io/github/release/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/stargazers"><img src="https://img.shields.io/github/stars/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
    <a href="https://github.com/JDK-Plus/spring-boot-starter-websocket/network/members"><img src="https://img.shields.io/github/forks/JDK-Plus/spring-boot-starter-websocket.svg" /></a>
</p>

- [中文文档](README-CN.md)

## Maven Dependencies

```xml
<dependency>
    <groupId>plus.jdk</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
    <version>1.0.4</version>
</dependency>
```

## Configuration

```
plus.jdk.websocket.enabled=true

# Specify host
plus.jdk.websocket.host=0.0.0.0

# Specify the websocket port
plus.jdk.websocket.port=10001


# Specify a custom-implemented validator
plus.jdk.websocket.session-authenticator=plus.jdk.broadcast.test.session.WSSessionAuthenticator

# The number of threads in the boss thread pool, the default is 1
plus.jdk.websocket.boss-loop-group-threads=1

# The number of worker thread pool threads, if not specified, the default is the number of CPU cores * 2
plus.jdk.websocket.worker-loop-group-threads=5

# Whether to allow cross-domain
plus.jdk.websocket.cors-allow-credentials=true

# cross-domain header
plus.jdk.websocket.cors-origins[0]=""

# Whether to use NioEventLoopGroup to handle requests
plus.jdk.websocket.use-event-executor-group=true

# Specify the number of NioEventLoopGroup thread pools
plus.jdk.websocket.event-executor-group-threads=0

# connection timeout
#plus.jdk.websocket.connect-timeout-millis=

# Specifies the maximum number of connections that the kernel queues for this socket
#plus.jdk.websocket.SO_BACKLOG=

# The spin count is used to control how many times the underlying socket.write(...) is called per Netty write operation
#plus.jdk.websocket.write-spin-count=

# log level
plus.jdk.websocket.log-level=debug
```

## Example of use

### Implement session authentication related logic according to your own business

#### Define the implementation of the business on the session

It is enough to implement the `IWsSession` interface for session authentication.

This interface encapsulates a `userId`, the parameter user can customize its type. There is also a channel, 
and subsequent user interactions with the object depend on this object

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

#### Custom `Session` validator

The authenticator must implement the `IWSSessionAuthenticator` interface. 
The usage example is as follows:

```java
import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import org.springframework.stereotype.Component;
import plus.jdk.websocket.common.HttpWsRequest;
import plus.jdk.websocket.global.IWSSessionAuthenticatorManager;
import plus.jdk.websocket.model.IWsSession;

@Component
public class WSSessionAuthenticator implements IWSSessionAuthenticatorManager<MyWsSession> {

    @Override
    public MyWsSession authenticate(Channel channel, FullHttpRequest req, String path) {
        HttpWsRequest httpWsRequest = new HttpWsRequest(req);
        String uid = httpWsRequest.getQueryValue("uid");
        return new MyWsSession(uid, channel);
    }

    @Override
    public void onSessionDestroy(IWsSession<MyWsSession> session) {
        IWSSessionAuthenticatorManager.super.onSessionDestroy(session);
    }
}
```

**Specify the validator component via configuration**

```
plus.jdk.websocket.session-authenticator=plus.jdk.broadcast.test.session.WSSessionAuthenticator
```


### Write relevant business logic

As above, after implementing Session authentication, you can continue to write the business logic of websocket.

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

### Actively push messages to user clients using websocket connections

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
     * The bean instance is already encapsulated at the bottom
     */
    @Resource
    private SessionGroupManager sessionGroupManager;

    @RequestMapping(value = "/message/send", method = {RequestMethod.GET})
    public Object sendMessage(@RequestParam String uid, @RequestParam String content){

        // Call the sessionGroupManager.getSession() function to get all connections of the current user in this instance.
        // You can implement your own session definition in the implementation of IWSSessionAuthenticator to distribute messages to different devices
        // Or report to the remote end which machines the current user is connected to
        ConcurrentLinkedDeque<IWsSession<?>> sessions = sessionGroupManager.getSession(uid, "/ws/message");
        for(IWsSession<?> wsSession: sessions) {
            wsSession.sendText(content); // 发送文本消息
            wsSession.sendBinary(content.getBytes()); // 发送二进制消息
        }
        return "success";
    }
}
```
