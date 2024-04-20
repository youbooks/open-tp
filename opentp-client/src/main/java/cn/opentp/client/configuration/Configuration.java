package cn.opentp.client.configuration;

import cn.opentp.core.net.OpentpMessage;
import cn.opentp.core.net.OpentpMessageConstant;
import cn.opentp.core.thread.pool.ThreadPoolContext;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Configuration {

    private volatile static Configuration INSTANCE;

    private Configuration() {
    }

    public static Configuration configuration() {
        if (INSTANCE == null) {
            synchronized (Configuration.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Configuration();
                }
            }
        }
        return INSTANCE;
    }


    public static final int DEFAULT_PORT = 9527;
    public static final String SERVER_SPLITTER = ",";
    public static final String SERVER_PORT_SPLITTER = ":";
    public static final OpentpMessage OPENTP_MSG_PROTO = new OpentpMessage(OpentpMessageConstant.MAGIC, OpentpMessageConstant.VERSION);

    // 服务器地址
    private final List<InetSocketAddress> serverAddresses = new CopyOnWriteArrayList<>();
    // 线程池增强对象缓存
    private final Map<String, ThreadPoolContext> ThreadPoolContextCache = new ConcurrentHashMap<>();
    // 线程池信息上报 bootstrap
    private Bootstrap bootstrap;
    // 线程池信息上报 socket
    private Channel threadPoolReportChannel;


    public Map<String, ThreadPoolContext> threadPoolContextCache() {
        return ThreadPoolContextCache;
    }

    public List<InetSocketAddress> serverAddresses() {
        return serverAddresses;
    }

    public Channel threadPoolReportChannel() {
        return threadPoolReportChannel;
    }

    public void setThreadPoolReportChannel(Channel threadPoolReportChannel) {
        this.threadPoolReportChannel = threadPoolReportChannel;
    }

    public Bootstrap bootstrap() {
        return bootstrap;
    }

    public void setBootstrap(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }
}
