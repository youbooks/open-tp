package cn.opentp.server.net;

import cn.opentp.server.net.handler.DemoHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.bootstrap.ServerBootstrapConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);

    public static Thread start() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ServerBootstrap serverBootstrap = new ServerBootstrap();
                serverBootstrap.group(new NioEventLoopGroup(10), new NioEventLoopGroup(10))
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel) throws Exception {
                                socketChannel.pipeline().addLast(new DemoHandler());
                            }
                        });
                serverBootstrap.bind(9527);
            }
        });
        log.info("net server start bind on 9527");
        thread.start();
        return thread;
    }
}
