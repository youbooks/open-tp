package cn.opentp.gossip.message;

import java.io.Serializable;

public class GossipMessage implements Serializable {

    // MessageTypeEnum
    public String type;
    public String data;
    public String cluster;
    public String from;

    public GossipMessage() {
    }

    public GossipMessage(String type, String data, String cluster, String from) {
        this.type = type;
        this.data = data;
        this.cluster = cluster;
        this.from = from;
    }

    public static GossipMessageBuilder builder() {
        return new GossipMessageBuilder();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
