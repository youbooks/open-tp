package cn.opentp.server.network.restful.netty.handler;

import cn.opentp.server.OpentpApp;
import cn.opentp.server.network.restful.RestfulService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestServiceNettyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Logger log = LoggerFactory.getLogger(RestServiceNettyHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest httpRequest) throws Exception {
        OpentpApp.instance().restfulService().handle(ctx, httpRequest);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Rest Service Netty Handler 捕获异常：", cause);
    }
}
