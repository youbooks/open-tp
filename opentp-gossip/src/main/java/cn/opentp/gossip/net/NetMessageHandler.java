package cn.opentp.gossip.net;

import cn.opentp.gossip.GossipManagement;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NetMessageHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        GossipManagement management = GossipManagement.instance();
        MessageService messageService = management.messageService();
        messageService.handleMsg(byteBuf);
    }
}
