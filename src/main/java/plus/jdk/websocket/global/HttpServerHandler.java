package plus.jdk.websocket.global;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import org.springframework.beans.TypeMismatchException;
import org.springframework.util.StringUtils;
import plus.jdk.websocket.common.WebsocketCommonException;
import plus.jdk.websocket.properties.WebsocketProperties;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final WebsocketProperties properties;

    private final WebsocketDispatcher websocketDispatcher;

    private final NioEventLoopGroup eventLoopGroup;

    public HttpServerHandler(WebsocketProperties properties, WebsocketDispatcher websocketDispatcher, NioEventLoopGroup worker) {
        this.properties = properties;
        this.websocketDispatcher = websocketDispatcher;
        this.eventLoopGroup = worker;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        try {
            handleHttpRequest(ctx, msg);
        } catch (TypeMismatchException e) {
            FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
            sendHttpResponse(ctx, msg, res);
            e.printStackTrace();
        } catch (Exception e) {
            FullHttpResponse res;
            res = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
            sendHttpResponse(ctx, msg, res);
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        websocketDispatcher.doOnError(ctx.channel(), cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        websocketDispatcher.doOnClose(ctx.channel());
        super.channelInactive(ctx);
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) throws WebsocketCommonException {
        FullHttpResponse res;
        // Handle a bad request.
        if (!req.decoderResult().isSuccess()) {
            res = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
            sendHttpResponse(ctx, req, res);
            return;
        }

        // Allow only GET methods.
        if (req.method() != GET) {
            res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN);
            sendHttpResponse(ctx, req, res);
            return;
        }

        HttpHeaders headers = req.headers();
        String host = headers.get(HttpHeaderNames.HOST);
        if (StringUtils.isEmpty(host)) {
            res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN);
            sendHttpResponse(ctx, req, res);
            return;
        }
        if (!StringUtils.isEmpty(properties.getHost()) && !properties.getHost().equals("0.0.0.0") && !properties.getHost().equals(host.split(":")[0])) {
            res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN);
            sendHttpResponse(ctx, req, res);
            return;
        }

        QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        String path = decoder.path();

        Channel channel = ctx.channel();

        WebsocketMethodMapping websocketMethodMapping = websocketDispatcher.getWebsocketMethodMap().get(path);
        if (websocketMethodMapping == null) {
            res = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            sendHttpResponse(ctx, req, res);
            return;
        }

        if (!req.headers().contains(UPGRADE) || !req.headers().contains(SEC_WEBSOCKET_KEY) || !req.headers().contains(SEC_WEBSOCKET_VERSION)) {
            res = new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN);
            sendHttpResponse(ctx, req, res);
            return;
        }

        String subprotocols = null;

        if (websocketDispatcher.hasBeforeHandshake(channel, path)) {
            websocketDispatcher.doBeforeHandshake(channel, req, path);
            if (!channel.isActive()) {
                return;
            }

            AttributeKey<String> subprotocolsAttrKey = AttributeKey.valueOf("subprotocols");
            if (channel.hasAttr(subprotocolsAttrKey)) {
                subprotocols = ctx.channel().attr(subprotocolsAttrKey).get();
            }
        }

        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), subprotocols, true, properties.getMaxFramePayloadLength());
        WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
        } else {
            ChannelPipeline pipeline = ctx.pipeline();
            pipeline.remove(ctx.name());
            if (properties.getReaderIdleTimeSeconds() != 0 || properties.getWriterIdleTimeSeconds() != 0 || properties.getAllIdleTimeSeconds() != 0) {
                pipeline.addLast(new IdleStateHandler(properties.getReaderIdleTimeSeconds(), properties.getWriterIdleTimeSeconds(), properties.getAllIdleTimeSeconds()));
            }
            if (properties.getUseCompressionHandler()) {
                pipeline.addLast(new WebSocketServerCompressionHandler());
            }
            pipeline.addLast(new WebSocketFrameAggregator(Integer.MAX_VALUE));
            if (properties.getUseEventExecutorGroup()) {
                pipeline.addLast(eventLoopGroup, new WebSocketServerHandler(websocketDispatcher));
            } else {
                pipeline.addLast(new WebSocketServerHandler(websocketDispatcher));
            }
            handshaker.handshake(channel, req).addListener(future -> {
                if (future.isSuccess()) {
                    websocketDispatcher.doOnOpen(channel, req, path);
                } else {
                    handshaker.close(channel, new CloseWebSocketFrame());
                }
            });
        }

    }

    private static void sendHttpResponse(
            ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        // Generate an error page if response getStatus code is not OK (200).
        int statusCode = res.status().code();
        if (statusCode != OK.code() && res.content().readableBytes() == 0) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
        }
        HttpUtil.setContentLength(res, res.content().readableBytes());

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || statusCode != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        String location = req.headers().get(HttpHeaderNames.HOST) + req.uri();
        return "ws://" + location;
    }

}
