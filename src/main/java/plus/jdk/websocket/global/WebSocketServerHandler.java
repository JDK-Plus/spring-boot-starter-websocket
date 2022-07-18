package plus.jdk.websocket.global;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import sun.util.logging.resources.logging;

@Slf4j
public class WebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final WebsocketDispatcher websocketDispatcher;

    private final BeanFactory beanFactory;

    public WebSocketServerHandler(WebsocketDispatcher websocketDispatcher, BeanFactory beanFactory) {
        this.websocketDispatcher = websocketDispatcher;
        this.beanFactory = beanFactory;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        SessionGroupManager sessionGroupManager = beanFactory.getBean(SessionGroupManager.class);
        sessionGroupManager.releaseChannel(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        handleWebSocketFrame(ctx, msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        websocketDispatcher.doOnError(ctx.channel(), cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        websocketDispatcher.doOnClose(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        websocketDispatcher.doOnEvent(ctx.channel(), evt);
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            websocketDispatcher.doOnMessage(ctx.channel(), frame);
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (frame instanceof CloseWebSocketFrame) {
            ctx.writeAndFlush(frame.retainedDuplicate()).addListener(ChannelFutureListener.CLOSE);
            return;
        }
        if (frame instanceof BinaryWebSocketFrame) {
            websocketDispatcher.doOnBinary(ctx.channel(), frame);
            return;
        }
        if (frame instanceof PongWebSocketFrame) {
            return;
        }
    }


}
