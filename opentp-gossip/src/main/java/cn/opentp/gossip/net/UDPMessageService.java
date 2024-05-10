package cn.opentp.gossip.net;

import cn.opentp.gossip.handler.*;
import cn.opentp.gossip.enums.MessageTypeEnum;
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
        bootstrap.group(eventLoopGroup).channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .option(ChannelOption.SO_RCVBUF, 2048 * 1024)
                .option(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        channel.pipeline().addLast(new NetMessageHandler());
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
    public void handle(ByteBuf data) {
//        JsonObject j = data.toJsonObject();
//        String msgType = j.getString(GossipMessageFactory.KEY_MSG_TYPE);
//        String _data = j.getString(GossipMessageFactory.KEY_DATA);
//        String cluster = j.getString(GossipMessageFactory.KEY_CLUSTER);
//        String from = j.getString(GossipMessageFactory.KEY_FROM);
//        if ((null == cluster || cluster.isEmpty()) || !GossipManager.getInstance().getCluster().equals(cluster)) {
//            log.error("This message shouldn't exist in my world!" + data);
//            return;
//        }

        String msgType = "";
        String cluster = "";
        String _data = "";
        String from = "";

        MessageHandler handler = null;
        MessageTypeEnum type = MessageTypeEnum.valueOf(msgType);
        if (type == MessageTypeEnum.SYNC_MESSAGE) {
            handler = new SyncMessageHandler();
        } else if (type == MessageTypeEnum.ACK_MESSAGE) {
            handler = new AckMessageHandler();
        } else if (type == MessageTypeEnum.ACK2_MESSAGE) {
            handler = new Ack2MessageHandler();
        } else if (type == MessageTypeEnum.SHUTDOWN) {
            handler = new ShutdownMessageHandler();
        } else if (type == MessageTypeEnum.REG_MESSAGE) {
            handler = new RegularMessageHandler();
        } else {
            log.error("Not supported message type");
        }
        if (handler != null) {
            handler.handle(cluster, _data, from);
        }
    }

    @Override
    public void send(String targetHost, Integer targetPort, Object message) {
        String json = JSON.toJSONString(message);
        DatagramPacket datagramPacket = new DatagramPacket(Unpooled.copiedBuffer(json, StandardCharsets.UTF_8), new InetSocketAddress(targetHost, targetPort));
        channel.writeAndFlush(datagramPacket);
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
