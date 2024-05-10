package cn.opentp.server.gossip;


import cn.opentp.gossip.GossipManagement;
import cn.opentp.gossip.GossipProperties;
import cn.opentp.gossip.GossipService;
import cn.opentp.gossip.net.MessageService;

public class Demo1 {

    public static void main(String[] args) {

        // 常用配置
        GossipProperties properties = new GossipProperties();
        properties.setCluster("opentp");
        properties.setHost("localhost");
        properties.setPort(9001);
        properties.setNodeId(null);
        properties.setClusterNodes("localhost:9002,localhost:9003");

        // 初始化
        GossipService.init(properties);

        // 开启服务
        GossipService.start();

        try {
            while (true) {
                Thread.sleep(5000);
                GossipManagement gossipManager = GossipManagement.instance();
                MessageService messageService = gossipManager.messageService();
                messageService.send("127.0.0.1", 9002, "hello");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
