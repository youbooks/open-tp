package cn.opentp.gossip.message.service;

import cn.opentp.gossip.enums.MessageTypeEnum;
import cn.opentp.gossip.message.MessagePayload;
import cn.opentp.gossip.message.handler.*;
import cn.opentp.gossip.message.service.netty.NettyMessageHandler;
import com.alibaba.fastjson2.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class UDPMessageService implements MessageService {

    private final Logger log = LoggerFactory.getLogger(UDPMessageService.class);

    private Channel channel;
    private EventLoopGroup eventLoopGroup;

    @Override
    public void start(String host, int port) {

        eventLoopGroup = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioDatagramChannel.class).option(ChannelOption.SO_BROADCAST, false).option(ChannelOption.SO_RCVBUF, 2048 * 1024).option(ChannelOption.SO_SNDBUF, 1024 * 1024).handler(new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                channel.pipeline().addLast(new NettyMessageHandler());
            }
        });

        ChannelFuture channelFuture = bootstrap.bind(host, port);
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("Socket bind success!");
                    channel = future.channel();
                } else {
                    log.error("An error occurred while bind the socket: ", future.cause());
                }
            }
        });
    }

    @Override
    public void handle(String data) {
//        log.debug("接收到消息：{}", data);
        MessagePayload gossipMessage = JSON.parseObject(data, MessagePayload.class);

        MessageHandler handler = null;
        MessageTypeEnum type = MessageTypeEnum.parse(gossipMessage.getType());
        if (type == MessageTypeEnum.SYNC) {
            handler = new SyncMessageHandler();
        } else if (type == MessageTypeEnum.ACK) {
            handler = new AckMessageHandler();
        } else if (type == MessageTypeEnum.ACK2) {
            handler = new Ack2MessageHandler();
        } else if (type == MessageTypeEnum.SHUTDOWN) {
            handler = new ShutdownMessageHandler();
        } else if (type == MessageTypeEnum.GOSSIP) {
            handler = new RegularMessageHandler();
        } else {
            log.error("Not supported message type");
        }
        if (handler != null) {
            handler.handle(gossipMessage.getCluster(), gossipMessage.getData(), gossipMessage.getFrom());
        }
    }

    @Override
    public void send(String targetHost, Integer targetPort, Object message) {
        if (message instanceof ByteBuf sendBuf) {
            ByteBuf realSendBuf = sendBuf.copy();
            DatagramPacket datagramPacket = new DatagramPacket(realSendBuf, new InetSocketAddress(targetHost, targetPort));
            channel.writeAndFlush(datagramPacket);
        } else {
            String json = JSON.toJSONString(message);
            DatagramPacket datagramPacket = new DatagramPacket(Unpooled.copiedBuffer(json, StandardCharsets.UTF_8), new InetSocketAddress(targetHost, targetPort));
            channel.writeAndFlush(datagramPacket);
        }
    }

    @Override
    public void close() {
        eventLoopGroup.shutdownGracefully();

        ChannelFuture channelFuture = channel.close();
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("Socket was close!");
                } else {
                    log.error("An error occurred while closing the socket: ", future.cause());
                }
            }
        });
    }
}
